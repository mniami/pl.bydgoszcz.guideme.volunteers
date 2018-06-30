package guideme.volunteers.ui.fragments.genericlist

import guideme.volunteers.R
import guideme.volunteers.domain.User
import guideme.volunteers.helpers.Container
import guideme.volunteers.helpers.dataservices.datasource.DataSourceId
import guideme.volunteers.helpers.dataservices.datasource.ObservableDataSource
import guideme.volunteers.ui.fragments.base.BaseFragment
import guideme.volunteers.ui.fragments.base.FragmentConfiguration
import guideme.volunteers.ui.fragments.base.ToolbarConfiguration
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.generic_view.*
import java.io.Serializable
import java.util.*

class GenericListFragmentImpl : BaseFragment<GenericPresenter>(), GenericListFragment {
    private val dataSourceContainer = Container.dataSourceContainer
    private val eventBusContainer = Container.eventBusContainer
    private var eventClickId: Serializable? = null
    private val database = Container.database
    private var currentUser = User()
    private var itemMap: GenericItemMap = EmptyGenericItemMap()
    private val mapperInstanceProvider = Container.mapperInstanceProvider

    init {
        presenter = GenericPresenter(this)
        configuration = FragmentConfiguration.withLayout(R.layout.generic_view).showBackArrow().create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun setArguments(args: Bundle?) {
        super.setArguments(args)
        loadArguments()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.string.menu_refresh -> {
                swipeRefresh.isRefreshing = true
                refreshAdapter()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        database.getCurrentUserAsync()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = { user ->
                    currentUser = user

                    actionButton.visibility = View.VISIBLE
                    if (recyclerView?.adapter == null) {
                        refreshAdapter()
                    }
                })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView?.let { rv ->
            rv.setHasFixedSize(true)
            rv.layoutManager = LinearLayoutManager(context)
        }
        actionButton?.setOnClickListener {
            itemMap.addItem()
        }
        swipeRefresh?.setOnRefreshListener {
            refreshAdapter()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView?

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(text: String?): Boolean {
                val genericAdapter = recyclerView?.adapter as GenericListAdapter?
                if (text != null) {
                    genericAdapter?.filter(text)
                }
                return true
            }

            override fun onQueryTextSubmit(text: String?): Boolean {
                return false
            }
        })
    }

    private fun refreshAdapter() {
        var genericItems = LinkedList<GenericItem<*>>()
        presenter?.items?.observeOn(AndroidSchedulers.mainThread())?.
                subscribeBy(
                        onNext = {
                            genericItems.add(it)
                        },
                        onError = {
                            Toast.makeText(this@GenericListFragmentImpl.context, "Ups", Toast.LENGTH_SHORT)
                        },
                        onComplete = {
                            val gi = genericItems
                            genericItems = LinkedList()
                            updateAdapter(gi)
                            swipeRefresh?.isRefreshing = false
                        })
    }

    private fun updateAdapter(list: List<GenericItem<*>>) {
        recyclerView?.adapter = GenericListAdapter(list) { item ->
            val lEventClickId = eventClickId
            if (item != null && lEventClickId != null) {
                val eventBus = eventBusContainer.get<GenericItemClickEvent>(lEventClickId)
                eventBus.post(GenericItemClickEvent(item))
            }
        }
        recyclerView?.invalidate()
    }

    private fun loadArguments() {
        val toolbarConfiguration = arguments?.get(GenericListFragment.TOOLBAR_CONFIGURATION)
        val dataSourceId = arguments?.get(GenericListFragment.DATA_SOURCE_ID)
        val mapperClassName = arguments?.get(GenericListFragment.MAPPER_CLASS_NAME)

        if (toolbarConfiguration is ToolbarConfiguration) {
            this.configuration.toolbar.titleResourceId = toolbarConfiguration.titleResourceId
            this.configuration.toolbar.showBackArrow = toolbarConfiguration.showBackArrow
        }

        if (dataSourceId is DataSourceId && mapperClassName is String) {
            val dataSource = dataSourceContainer.getDataSource(dataSourceId)

            if (dataSource != null && dataSource.isObservableDataSource() && dataSource is ObservableDataSource<*>) {
                if (mapperClassName != null) {
                    val mapperInstance = mapperInstanceProvider.get<GenericItemMap>(mapperClassName)
                    if (mapperInstance != null) {
                        itemMap = mapperInstance
                        presenter?.items = itemMap.map(dataSource.data.observable)
                    }
                } else {
                    val items = dataSource.data.observable as Observable<GenericItem<*>>?
                    if (items != null) {
                        presenter?.items = items
                    }
                }
            }
        }
    }
}

