package org.noma

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Pine=Color(0xFF173F38)
internal val Cream=Color(0xFFF8F5EE)
internal val Apricot=Color(0xFFF2BA96)

@Composable internal fun NomaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme=lightColorScheme(primary=Pine,onPrimary=Color.White,primaryContainer=Color(0xFFDCE8DC),
            onPrimaryContainer=Pine,secondary=Color(0xFF79543F),secondaryContainer=Color(0xFFF5DDC8),
            background=Cream,surface=Cream,surfaceContainer=Color(0xFFF0EDE5),surfaceContainerLow=Color(0xFFFEFCF7),
            surfaceVariant=Color(0xFFE8E9DF),onSurface=Color(0xFF243C35),onSurfaceVariant=Color(0xFF58645B),
            outline=Color(0xFF7B877C),outlineVariant=Color(0xFFD8DDD1)),
        typography=Typography(
            headlineLarge=TextStyle(fontFamily=FontFamily.Serif,fontSize=36.sp,lineHeight=40.sp),
            headlineMedium=TextStyle(fontFamily=FontFamily.Serif,fontSize=30.sp,lineHeight=35.sp),
            titleLarge=TextStyle(fontFamily=FontFamily.Serif,fontSize=25.sp,lineHeight=30.sp),
            titleMedium=TextStyle(fontWeight=FontWeight.Medium,fontSize=17.sp,lineHeight=24.sp)),
        shapes=Shapes(small=RoundedCornerShape(12.dp),medium=RoundedCornerShape(20.dp),large=RoundedCornerShape(28.dp)),
        content=content)
}

/** Simple local vector marks; decorative, with labels supplied by the navigation items. */
@Composable internal fun NavMark(index: Int) {
    val color=LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val w=size.width;val stroke=Stroke(1.8.dp.toPx())
        when(index) {
            0 -> { drawCircle(color,w*.24f,Offset(w*.5f,w*.5f),style=stroke)
                for(i in 0..7) { val a=i*Math.PI/4;drawLine(color,Offset(w*.5f+Math.cos(a).toFloat()*w*.37f,w*.5f+Math.sin(a).toFloat()*w*.37f),Offset(w*.5f+Math.cos(a).toFloat()*w*.46f,w*.5f+Math.sin(a).toFloat()*w*.46f),1.8.dp.toPx()) } }
            1 -> { val p=Path().apply { moveTo(w*.22f,w*.12f);cubicTo(w*.95f,w*.15f,w*.05f,w*.8f,w*.78f,w*.87f) };drawPath(p,color,style=stroke);drawCircle(color,w*.07f,Offset(w*.22f,w*.12f));drawCircle(color,w*.07f,Offset(w*.78f,w*.87f)) }
            2 -> { drawCircle(color,w*.16f,Offset(w*.32f,w*.32f),style=stroke);drawCircle(color,w*.16f,Offset(w*.72f,w*.32f),style=stroke);drawArc(color,180f,180f,false,Offset(w*.1f,w*.58f),Size(w*.43f,w*.4f),style=stroke);drawArc(color,180f,180f,false,Offset(w*.5f,w*.58f),Size(w*.43f,w*.4f),style=stroke) }
            else -> { val p=Path().apply { moveTo(w*.5f,w*.08f);lineTo(w*.85f,w*.23f);lineTo(w*.82f,w*.6f);quadraticTo(w*.72f,w*.82f,w*.5f,w*.93f);quadraticTo(w*.28f,w*.82f,w*.18f,w*.6f);lineTo(w*.15f,w*.23f);close() };drawPath(p,color,style=stroke);drawLine(color,Offset(w*.34f,w*.49f),Offset(w*.46f,w*.61f),2.dp.toPx());drawLine(color,Offset(w*.46f,w*.61f),Offset(w*.68f,w*.36f),2.dp.toPx()) }
        }
    }
}
