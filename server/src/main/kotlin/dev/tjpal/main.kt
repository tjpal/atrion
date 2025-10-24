package dev.tjpal

import dev.tjpal.di.DaggerAppComponent

fun main() {
    val app  = DaggerAppComponent.create().app()
    app.run()
}
