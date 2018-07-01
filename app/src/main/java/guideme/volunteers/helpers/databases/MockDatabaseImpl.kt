package guideme.volunteers.helpers.databases

import guideme.volunteers.domain.Action
import guideme.volunteers.domain.User
import guideme.volunteers.domain.Volunteer
import guideme.volunteers.helpers.dataservices.databases.Database
import guideme.volunteers.helpers.dataservices.databases.IDatabaseListener
import guideme.volunteers.helpers.dataservices.datasource.MockUserDataSource
import guideme.volunteers.helpers.dataservices.datasource.MockVolunteersDataSource
import android.util.Log
import io.reactivex.Observable

/**
 * Created by DASZ on 10.02.2018.
 */
class MockDatabaseImpl : Database {
    val logName = "MockDatabase"

    override fun updateVolunteer(volunteer: Volunteer): Observable<Volunteer> {
        Log.d(logName, "update volunteer called ${volunteer.person.name}")
        return Observable.create {
            Thread.sleep(200)
            volunteer.person.activity.actions.add(Action("Updated data"))
            it.onNext(volunteer)
            it.onComplete()
        }
    }

    override fun getCurrentUserAsync(): Observable<User> = MockUserDataSource().data.observable

    override fun addListener(databaseListener: IDatabaseListener) {
    }

    override fun removeListener(databaseListener: IDatabaseListener) {
    }

    override fun getUser(name: String): Observable<User> = MockUserDataSource().data.observable

    override fun getVolunteers(): Observable<Volunteer> = MockVolunteersDataSource().data.observable

    override fun init() {
    }

    override fun signOut() {
    }

    override fun setUser(user: User): Observable<User> = MockUserDataSource().data.observable
}