import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

object RequestQueue {

    private val requests = LinkedList<Pair<String, String>>()
    private val responses = LinkedList<Pair<String, String>>()

    private val requestLocked = AtomicBoolean(false)
    private val responseLocked = AtomicBoolean(false)

    fun addRequest(requester: String, request: String): String {
        while (requestLocked.get() || responseLocked.get()) {}

        requestLocked.set(true)
        responseLocked.set(true)

        val id = System.nanoTime().toString() + "_$requester"
        requests += Pair(id, request)
        println("ADDING REQUEST: $id $request")

        requestLocked.set(false)
        responseLocked.set(false)
        return id
    }

    fun finishRequest(id: String, response: String) {
        println("WAITING FOR UNLOCKS ${requestLocked.get()}  ${responseLocked.get()}")
        while (requestLocked.get() || responseLocked.get()) {}
        println("UNLOCKED: ADDED $response FOR $id")
        requestLocked.set(true)
        responseLocked.set(true)

        requests.removeIf { request -> request.first == id }
        responses += Pair(id, response)

//        Thread.sleep(50)

        requestLocked.set(false)
        responseLocked.set(false)
    }

    fun containsResponse(id: String): Boolean {
        while (requestLocked.get() || responseLocked.get()) {}

        requestLocked.set(true)
        responseLocked.set(true)
        var containsResponse = false

        responses.forEach { response ->
            if (response.first == id) {
                containsResponse = true
            }
        }

        requestLocked.set(false)
        responseLocked.set(false)
        return containsResponse
    }

    fun takeRequestIfAvailable(id: String): Pair<String, String>? {
        while (requestLocked.get() || responseLocked.get()) {}

        requestLocked.set(true)
        responseLocked.set(true)

        if (requests.size != 0){
            println(requests.size)
        }

        var request: Pair<String, String>? = null

        for (item in requests) {
            if (item.first.contains(id)) {
                request = item
                break
            }
        }

//        val request = requests.find { request -> request.first.contains(id) }

        requestLocked.set(false)
        responseLocked.set(false)
        return request
    }

    fun takeResponseIfAvailable(id: String): String {
        while (requestLocked.get() || responseLocked.get()) {}

        requestLocked.set(true)
        responseLocked.set(true)

        val response = responses.find { r -> r.first == id } ?: return ""
        responses.removeIf { r -> r.first == id }

        requestLocked.set(false)
        responseLocked.set(false)
        return response.second
    }

}