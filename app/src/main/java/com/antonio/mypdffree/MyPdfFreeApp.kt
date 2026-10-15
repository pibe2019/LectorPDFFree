package com.antonio.mypdffree

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/*
 * Sin @ApplicationScope (a diferencia de GuiaMedicaSebas): ningun repositorio mantiene flujos calientes
 * a nivel de app. Se añade el dia que algo lo necesite, no antes.
 */
@HiltAndroidApp
class MyPdfFreeApp : Application()
