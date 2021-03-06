package guideme.volunteers.ui.activities.main

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.widget.SearchView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import guideme.volunteers.R
import guideme.volunteers.auth.SignInAuthResult
import guideme.volunteers.domain.Human
import guideme.volunteers.domain.Project
import guideme.volunteers.domain.Volunteer
import guideme.volunteers.helpers.Container
import guideme.volunteers.helpers.dataservices.errors.ErrorMessage
import guideme.volunteers.log.createLog
import guideme.volunteers.ui.activities.main.base.BaseMainActivityImpl
import guideme.volunteers.ui.fragments.base.FragmentConfiguration
import guideme.volunteers.ui.tools.ToolbarConfigurationHandler
import guideme.volunteers.ui.views.actionbar.ActionBarTool
import guideme.volunteers.ui.views.actionbar.ActionBarToolImpl
import kotlinx.android.synthetic.main.activity_main.*

internal class MainActivityImpl : BaseMainActivityImpl(), MainView {
    private val log = createLog(this)
    override val actionBarTool: ActionBarTool = ActionBarToolImpl(this)

    private var menu: Menu? = null
    private var presenter: MainPresenter? = null
    private val fragmentChanger = Container.fragmentChanger
    private val toolbarConfigurationHandler = ToolbarConfigurationHandler()
    private val configuration: FragmentConfiguration = FragmentConfiguration
            .withTitle(R.string.volunteers_title)
            .noArrow()
            .create()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        log.d { "onActivityResult" }
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null) {
            Container.googleAuth.onActivityResult(requestCode, data)
            refreshSignInLayout()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        log.d { "onCreate" }
        fragmentChanger.supportFragmentManager = supportFragmentManager

        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            log.d { "savedInstance == null" }
            Container.googleAuth.init(this)

            setContentView(R.layout.activity_main)

            val myToolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(myToolbar)

            if (presenter == null) {
                presenter = MainPresenter(this, Container.googleAuth, Container.database, this)
            }
            presenter?.onCreate()

            supportFragmentManager?.addOnBackStackChangedListener {
                refreshMenu()
                refreshSignInLayout()
            }
            signInButton.setOnClickListener {
                presenter?.onAuthenticationClick()
            }
        }
    }


    override fun onStart() {
        log.d { "onStart" }
        fragmentChanger.paused = false
        super.onStart()
        presenter?.onStart()
    }

    override fun onPause() {
        log.d { "onPause" }
        super.onPause()
        fragmentChanger.paused = true
    }

    override fun getResourceText(id: Int): String = getString(id)

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)

        this.menu = menu

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView?

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(text: String?): Boolean = false

            override fun onQueryTextSubmit(text: String?): Boolean = true
        })
        refreshMenu()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                presenter?.onSettingsClick()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun refreshMenu() {
        log.d { "refreshMenu" }

        if (supportFragmentManager.backStackEntryCount == 0) {
            toolbarConfigurationHandler.applyConfiguration(this, configuration)
        }
    }

    override fun hideProgress() {
        progress.visibility = View.GONE
    }

    override fun showProgress() {
        progress.visibility = View.VISIBLE
    }

    override fun openSettings() = fragmentChanger.openSettings()
    override fun openAuthentication() = fragmentChanger.openAuthentication()
    override fun showVolunteerList() = fragmentChanger.showVolunteerList()
    override fun openHome() = fragmentChanger.openHome()
    override fun showProject(project: Project) = fragmentChanger.showProject(project)
    override fun showVolunteer(volunteer: Volunteer) = fragmentChanger.showVolunteer(volunteer)
    override fun openEditUserDetails(human: Human) = fragmentChanger.openEditUserDetails(human)
    override fun updateUserStatus(signInResult: SignInAuthResult) {}
    override fun showError(errorMessage: ErrorMessage) {
        val content = errorMessage.content ?: "Unexpected error occurred"
        Snackbar.make(activity_main, content, Snackbar.LENGTH_LONG).show()
    }

    private fun refreshSignInLayout() {
        signInLayout.visibility = if (Container.googleAuth.isSignedIn()) View.GONE else View.VISIBLE
    }
}