package dev.tjpal.api.route

/**
 * Holder used to expose a RouteRegistrar instance to parts of the system that are not wired
 * into Ktor's Application startup. The application should call setRegistrar(...) during initialization.
 *
 * We can't use dagger here since the ktor component becomes available later.
 */
object RouteRegistrarHolder {
    @Volatile
    private var registrar: KtorRouteRegistrar? = null

    fun setRegistrar(registrar: KtorRouteRegistrar) {
        this.registrar = registrar
    }

    fun getRegistrar(): KtorRouteRegistrar = registrar ?: throw IllegalStateException("RouteRegistrar not set. Ensure you create and set a KtorRouteRegistrar at application startup.")
}
