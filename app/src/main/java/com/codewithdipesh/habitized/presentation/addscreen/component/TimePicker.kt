package com.codewithdipesh.habitized.presentation.addscreen.component

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun TimePicker(
    modifier: Modifier = Modifier,
    width:Dp,
    itemHeight:Dp,
    numberOfDisplayItems:Int,
    items:List<Int>,
    initialItem:Int,
    itemScaleFont :Float = 1.5f,
    fontSize : Int,
    textFont: FontFamily,
    textWeight : FontWeight,
    selectedTextColor:Color,
    nonSelectedTextColor :Color,
    onItemSelected:(Int)->Unit
) {

    val itemHalfHeight = LocalDensity.current.run { itemHeight.toPx()/2 }

    val scrollState = rememberLazyListState()
    var lastSelectedIndex by remember {
        mutableStateOf(0)
    }
    var itemState by remember {
        mutableStateOf(items)
    }

    LaunchedEffect(items){
        itemState = items
        val cycles = 100000
        val itemCount = items.size * cycles
        val halfCycles = cycles / 2
        val index = items.indexOf(initialItem)
        val targetIndex = (halfCycles * items.size) + index
        scrollState.scrollToItem(targetIndex)
        lastSelectedIndex = targetIndex
    }

    LazyColumn(
        modifier = Modifier
            .width(width)
            .height(itemHeight * numberOfDisplayItems),
        state = scrollState,
        flingBehavior = rememberSnapFlingBehavior(
            lazyListState = scrollState
        )
    ){
        items(
            count = items.size * 100000
        ){ i->
            val item = items[i%items.size]

            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth()
                    .onGloballyPositioned{coordinates ->
                        val y = coordinates.positionInParent().y - itemHalfHeight
                        val center = (coordinates.parentCoordinates?.size?.height ?: 0) / 2f
                        val isSelected =
                           y > (center - itemHalfHeight) && y < (center + itemHalfHeight)
                        if(isSelected && lastSelectedIndex != i){
                            lastSelectedIndex = i
                            onItemSelected(item)
                        }
                    },
                contentAlignment = Alignment.Center
            ){
                Text(
                    text = String.format("%02d", item),
                    style = TextStyle(
                        fontFamily = textFont,
                        fontWeight = textWeight,
                        color = if (lastSelectedIndex == i) selectedTextColor
                        else nonSelectedTextColor,
                        fontSize = if (lastSelectedIndex == i) fontSize.sp
                        else fontSize.sp / itemScaleFont
                    )
                )
            }
        }
    }

}