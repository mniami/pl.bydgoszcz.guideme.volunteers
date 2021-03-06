package guideme.volunteers.ui.fragments.volunteer.details.users

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.text.Html
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.squareup.picasso.Picasso
import guideme.volunteers.R
import guideme.volunteers.domain.Privilege
import guideme.volunteers.domain.Volunteer
import guideme.volunteers.helpers.Container
import guideme.volunteers.helpers.dataservices.datasource.UserDataSource
import guideme.volunteers.helpers.dataservices.errors.ErrorMessage
import guideme.volunteers.helpers.dataservices.errors.ErrorType
import guideme.volunteers.ui.fragments.base.BaseFragment
import guideme.volunteers.ui.fragments.base.FragmentConfiguration
import guideme.volunteers.ui.fragments.volunteer.details.VolunteerProjectListFragment
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.volunteer_details_account.*
import kotlinx.android.synthetic.main.volunteer_details_fragment.*

class VolunteerDetailsFragment : BaseFragment<VolunteerDetailsPresenter>(), IVolunteerDetailsFragment {
    companion object {
        val VOLUNTEER_ARG = "volunteer"
    }

    private var actionEdit: MenuItem? = null
    private var actionDelete: MenuItem? = null

    init {
        presenter = VolunteerDetailsPresenter(this)
        configuration = FragmentConfiguration
                .withLayout(R.layout.volunteer_details_fragment)
                .withMenu(R.menu.volunteers_details_menu)
                .showBackArrow()
                .create()
    }

    override fun onStart() {
        super.onStart()

        val volunteer = presenter?.volunteer
        if (volunteer == null){
            return
        }
        Container.database.getVolunteers()
                .filter { it -> it.id == volunteer.id }
                .subscribeBy (
                        onNext = {
                            presenter?.volunteer = it
                        },
                        onComplete = {
                            updateView()
                        }
                )
    }

    private fun updateView() {
        actionBar.hideOptions()
        val volunteer = presenter?.volunteer
        if (volunteer == null) {
            return
        }
        presenter?.volunteer?.let { v ->
            val address = v.person.addresses.entries.firstOrNull()
            var subHeader = v.person.email
            if (address != null) {
                subHeader += getString(R.string.person_short_description)
                subHeader = subHeader.replace("%city", address.value.city).replace("%zip", address.value.zip).replace("%street", address.value.street)
            }
            actionBar.setTitle("${v.person.name} ${v.person.surname}")
            tvSubHeader?.text = Html.fromHtml(subHeader)

            tvHeader?.text = "${v.person.name} ${v.person.surname}"

            if (v.person.avatarImageUri.isNotEmpty()) {
                Picasso.with(context).load(v.person.avatarImageUri).into(ivImage)
            }

            tvShortDescription?.text = v.person.personalityDescription
            tvDescription?.text = v.person.description

            val userDataSource = Container.dataSourceContainer.getDataSource(UserDataSource.ID) as UserDataSource?
            userDataSource?.let {
                it.item.observable.subscribeBy(onNext = { currentUser ->
                    val visible = currentUser.person.privilege == Privilege.ADMIN
                    actionEdit?.isVisible = visible
                    actionDelete?.isVisible = visible
                })
            }
        }

        viewPager?.let {
            it.adapter = object : FragmentPagerAdapter(childFragmentManager) {
                override fun getCount(): Int {
                    return 1
                }

                override fun getItem(position: Int): Fragment? {
                    return presenter?.volunteer?.let { volunteer ->
                        var fragment: Fragment? = null
                        when (position) {
                            0 -> fragment = VolunteerDetailsAccountFragment()
                            1 -> fragment = VolunteerProjectListFragment()
                        }
                        val bundle = Bundle()
                        bundle.putSerializable("volunteer", volunteer)
                        fragment?.arguments = bundle
                        return@let fragment
                    }
                }

                override fun getPageTitle(position: Int): CharSequence {
                    when (position) {
                        0 -> return getString(R.string.details_label)
                        1 -> return getString(R.string.projects_label)
                    }
                    return ""
                }
            }
            slidingTabLayout?.setViewPager(it)
        }
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        presenter?.let {
            it.volunteer = args?.get(VOLUNTEER_ARG) as Volunteer?
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)

        actionEdit = menu?.findItem(R.id.action_edit)
        actionDelete = menu?.findItem(R.id.action_delete)

        val v = presenter?.volunteer
        if (v == null) {
            return
        }

        actionDelete?.setOnMenuItemClickListener {
            Container.database.deleteVolunteer(v)
                    .subscribeBy(
                            onSuccess = {
                                mainActivity.goBack()
                            },
                            onError = {
                                mainActivity.showError(ErrorMessage(ErrorType.DELETE_FAILED, "Delete Volunteer failed"))
                            })

            return@setOnMenuItemClickListener true
        }
        actionEdit?.setOnMenuItemClickListener {
            mainActivity.openEditUserDetails(v)
            return@setOnMenuItemClickListener true
        }
    }
}