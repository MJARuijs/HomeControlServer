package networking.nio

import java.nio.channels.Selector

interface Registrable {

    fun register(selector: Selector)

}