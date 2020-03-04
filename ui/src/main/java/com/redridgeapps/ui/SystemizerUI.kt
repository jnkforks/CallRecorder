package com.redridgeapps.ui

import androidx.compose.Composable
import androidx.compose.MutableState
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.Text
import androidx.ui.foundation.DrawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.material.Button
import androidx.ui.material.CircularProgressIndicator
import androidx.ui.material.MaterialTheme
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import androidx.ui.unit.sp
import com.redridgeapps.repository.viewmodel.ISystemizerViewModel
import com.redridgeapps.ui.initialization.Destination
import com.redridgeapps.ui.initialization.UIInitializer
import com.redridgeapps.ui.utils.fetchViewModel
import javax.inject.Inject

object SystemizerDestination : Destination {

    override val uiInitializer = SystemizerUIInitializer::class.java
}

class SystemizerUIInitializer @Inject constructor() : UIInitializer {

    @Composable
    override fun initialize() {
        val viewModel = fetchViewModel<ISystemizerViewModel>()
        SystemizerUI(viewModel)
    }
}

@Composable
fun SystemizerUI(viewModel: ISystemizerViewModel) {
    Column(DrawBackground(MaterialTheme.colors().primary) + LayoutPadding(20.dp)) {
        val isSystemized = state { checkIsSystemized(viewModel) }

        ExplanationText(isSystemized)

        Spacer(LayoutHeight(40.dp))

        SystemizationButton(viewModel, isSystemized)
    }
}

@Composable
fun ColumnScope.ExplanationText(isSystemized: MutableState<Boolean>) {
    val text = if (isSystemized.value) "App is Systemized."
    else "App is not a system app. Call Recording only works with System apps."

    Container(LayoutFlexible(0.8F), alignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography().h3.copy(Color.White))
    }
}

@Composable
fun ColumnScope.SystemizationButton(
    viewModel: ISystemizerViewModel,
    isSystemized: MutableState<Boolean>
) {

    val backgroundColor: Color
    val text: String

    var inProgress by state { false }

    if (isSystemized.value) {
        backgroundColor = Color.Red
        text = "Unsystemize"
    } else {
        backgroundColor = MaterialTheme.colors().secondary
        text = "Systemize"
    }

    val onClick = {
        inProgress = true
        if (!isSystemized.value) {
            viewModel.systemize {
                isSystemized.value = checkIsSystemized(viewModel)
                inProgress = false
            }
        } else {
            viewModel.unSystemize {
                isSystemized.value = checkIsSystemized(viewModel)
                inProgress = false
            }
        }
    }

    Container(LayoutFlexible(0.2F) + LayoutWidth.Fill, alignment = Alignment.Center) {
        if (!inProgress) {
            Button(LayoutWidth.Fill, backgroundColor = backgroundColor, onClick = onClick) {
                Text(text = text, style = TextStyle(fontSize = 25.sp))
            }
        } else {
            CircularProgressIndicator(MaterialTheme.colors().secondary)
        }
    }
}

fun checkIsSystemized(viewModel: ISystemizerViewModel) = viewModel.isAppSystemized()
