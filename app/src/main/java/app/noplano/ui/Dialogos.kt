package app.noplano.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun DialogoBase(fechar: () -> Unit, conteudo: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = fechar) {
        Column(Modifier.fillMaxWidth().background(Cores.superficie).padding(20.dp)) { conteudo() }
    }
}

@Composable
fun DialogoTexto(
    titulo: String, rotulo: String, inicial: String, fechar: () -> Unit,
    numerico: Boolean = false, sufixo: String? = null, validar: (String) -> Boolean = { it.isNotBlank() }, confirmar: (String) -> Unit,
) {
    var texto by remember { mutableStateOf(inicial) }
    DialogoBase(fechar) {
        Text(titulo.uppercase(), style = Tipos.tituloCartao)
        Espaco(12.dp)
        CampoTexto(texto, { texto = it }, rotulo, numerico = numerico, sufixo = sufixo)
        Espaco(16.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotaoContorno("Cancelar", fechar, Modifier.weight(1f))
            BotaoPrincipal("Salvar", { confirmar(texto) }, Modifier.weight(1f), ativo = validar(texto))
        }
    }
}

@Composable
fun DialogoConfirmar(titulo: String, texto: String, acao: String, fechar: () -> Unit, confirmar: () -> Unit) {
    DialogoBase(fechar) {
        Text(titulo.uppercase(), style = Tipos.tituloCartao)
        Espaco(8.dp)
        Text(texto, style = Tipos.corpoSuave)
        Espaco(16.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotaoContorno("Cancelar", fechar, Modifier.weight(1f))
            BotaoContorno(acao, confirmar, Modifier.weight(1f), cor = Cores.erro)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoHora(titulo: String, minutos: Int, fechar: () -> Unit, confirmar: (Int) -> Unit) {
    val estado = rememberTimePickerState(minutos / 60, minutos % 60, is24Hour = true)
    DialogoBase(fechar) {
        Text(titulo.uppercase(), style = Tipos.tituloCartao)
        Espaco(16.dp)
        TimePicker(estado, Modifier.align(Alignment.CenterHorizontally))
        Espaco(12.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotaoContorno("Cancelar", fechar, Modifier.weight(1f))
            BotaoPrincipal("Salvar", { confirmar(estado.hour * 60 + estado.minute) }, Modifier.weight(1f))
        }
    }
}
