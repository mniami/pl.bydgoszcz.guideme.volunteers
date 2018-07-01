package guideme.volunteers.eventbus

import io.reactivex.Observable
import io.reactivex.subjects.Subject
import io.reactivex.subjects.PublishSubject
import java.io.Serializable
import java.util.*

class EventBusContainer {
    val map : MutableMap<Serializable, EventBus<*>> = HashMap()

    fun <T> get(key : Serializable) : EventBus<T>{
        if (map.containsKey(key)) {
            return map[key] as EventBus<T>
        }
        else {
            val eventBus = EventBus<T>()
            map.put(key, eventBus)
            return eventBus
        }
    }

    fun disposeEventBus(key : Serializable){
        map.remove(key)
    }
}

class EventBus<T> @JvmOverloads constructor(private val subject: Subject<T> = PublishSubject.create<T>()) {

    fun <E : T> post(event: E) {
        subject.onNext(event)
    }

    fun observe(): Observable<T> {
        return subject
    }

    fun <E : T> observeEvents(eventClass: Class<E>): Observable<E> {
        return subject.ofType(eventClass)//pass only events of specified type, filter all other
    }
}