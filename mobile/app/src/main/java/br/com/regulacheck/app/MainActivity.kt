package br.com.regulacheck.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.regulacheck.app.ui.navegacao.AppRegulaCheck
import br.com.regulacheck.app.ui.tema.TemaRegulaCheck

/** Única Activity: barras do sistema claras (o app só tem modo claro) e a interface Compose. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val dependencias = (application as RegulaCheckApp).dependencias
        setContent {
            TemaRegulaCheck { AppRegulaCheck(dependencias) }
        }
    }
}
