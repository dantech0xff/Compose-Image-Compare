package com.creative.androidimagecompare

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import kotlinx.coroutines.launch

/**
 * Created by dan on 23/02/2024
 *
 * Copyright © 2024 1010 Creative. All rights reserved.
 */

const val AnimationTime = 300

@Composable
fun ReImage(modifier: Modifier = Modifier) {
    var splitPosition by remember { mutableFloatStateOf(0.45f) }
    var leftImagePath by remember { mutableStateOf<Uri?>(null) }
    var rightImagePath by remember { mutableStateOf<Uri?>(null) }

    val leftPhotoPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) {
        leftImagePath = it
    }
    val rightPhotoPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) {
        rightImagePath = it
    }

    var leftImageBitmap by remember {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    val leftImageAlpha = remember {
        Animatable(0f)
    }
    var rightImageBitmap by remember {
        mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    }
    val rightImageAlpha = remember {
        Animatable(0f)
    }
    var whRatio by remember {
        mutableFloatStateOf(3f / 4f)
    }

    val context = LocalContext.current

    LaunchedEffect(key1 = leftImagePath, key2 = rightImagePath) {
        launch { leftImageAlpha.animateTo(0.0f, tween(AnimationTime, easing = LinearEasing)) }
        // load the image bitmap from the uri
        leftImageBitmap = (context.imageLoader.execute(
            ImageRequest.Builder(context)
                .data(leftImagePath)
                .scale(Scale.FILL)
                .precision(Precision.INEXACT)
                .build()
        ).drawable as? BitmapDrawable)?.bitmap?.asImageBitmap()

        launch { leftImageAlpha.animateTo(1.0f, tween(AnimationTime, easing = LinearEasing)) }

        launch { rightImageAlpha.animateTo(0.0f, tween(AnimationTime, easing = LinearEasing)) }
        rightImageBitmap = (context.imageLoader.execute(
            ImageRequest.Builder(context)
                .data(rightImagePath)
                .scale(Scale.FILL)
                .precision(Precision.INEXACT)
                .build()
        ).drawable as? BitmapDrawable)?.bitmap?.asImageBitmap()
        launch { rightImageAlpha.animateTo(1.0f, tween(AnimationTime, easing = LinearEasing)) }
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                if (leftImagePath == null && rightImagePath == null) {
                    "Load images to compare"
                } else {
                    if (leftImagePath == null) {
                        "Load left image to compare"
                    } else if (rightImagePath == null) {
                        "Load right image to compare"
                    } else {
                        "Drag the line to compare images"
                    }
                },
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(8.dp)
            )
        }

        item {
            Canvas(modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .aspectRatio(whRatio)
                .shadow(Color.Black.copy(0.1f), 20.dp, 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        val newPosition = (change.position.x / size.width).coerceIn(0f, 1f)
                        splitPosition = newPosition
                    }
                }
            ) {
                val width = size.width
                val height = size.height

                // Calculate rectangle dimensions based on touch position
                val rect1Width = (width * splitPosition).toInt()
                val rect2Width = width - rect1Width

                drawRect(
                    color = Color.White,
                    topLeft = Offset(0f, 0f),
                    size = Size(rect1Width.toFloat(), height)
                )

                leftImageBitmap?.let {
                    val cropWidth: Int
                    val cropHeight: Int
                    val imgWhRatio = it.width.toFloat() / it.height.toFloat()
                    if (imgWhRatio > whRatio) {
                        cropWidth = (it.height * whRatio).toInt()
                        cropHeight = it.height
                    } else {
                        cropWidth = it.width
                        cropHeight = (it.width / whRatio).toInt()
                    }

                    drawImage(
                        it,
                        srcOffset = IntOffset(
                            x = (it.width - cropWidth) / 2,
                            y = (it.height - cropHeight) / 2
                        ),
                        srcSize = IntSize((cropWidth * splitPosition).toInt(), cropHeight),
                        dstOffset = IntOffset(0, 0),
                        dstSize = IntSize((width * splitPosition).toInt(), height.toInt()),
                        alpha = leftImageAlpha.value
                    )
                }

                drawRect(
                    color = Color.LightGray,
                    topLeft = Offset(rect1Width.toFloat(), 0f),
                    size = Size(rect2Width, height)
                )
                rightImageBitmap?.let {
                    val cropWidth: Int
                    val cropHeight: Int
                    val imgWhRatio = it.width.toFloat() / it.height.toFloat()
                    if (imgWhRatio > whRatio) {
                        cropWidth = (it.height * whRatio).toInt()
                        cropHeight = it.height
                    } else {
                        cropWidth = it.width
                        cropHeight = (it.width / whRatio).toInt()
                    }

                    drawImage(
                        it,
                        srcOffset = IntOffset(
                            x = (it.width - cropWidth) / 2 + (cropWidth * splitPosition).toInt(),
                            y = (it.height - cropHeight) / 2
                        ),
                        srcSize = IntSize((cropWidth * (1 - splitPosition)).toInt(), cropHeight),
                        dstOffset = IntOffset(rect1Width, 0),
                        dstSize = IntSize((width * (1 - splitPosition)).toInt(), height.toInt()),
                        alpha = rightImageAlpha.value
                    )
                }

                drawLine(
                    color = Color.White,
                    start = Offset(rect1Width.toFloat(), 5f),
                    end = Offset(rect1Width.toFloat(), height - 5f),
                    strokeWidth = 10f,
                    cap = StrokeCap.Round
                )
            }
        }

        item {
            Spacer(modifier = Modifier.padding(8.dp))
        }

        item {
            Column(
                modifier = Modifier
                    .shadow(color = Color.Black.copy(0.1f), 20.dp, 12.dp)
                    .background(color = White, shape = RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .wrapContentHeight()
                ) {
                    Button(
                        onClick = {
                            leftPhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text("Load Left Image")
                    }

                    Button(
                        onClick = {
                            rightPhotoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text("Load Right Image")
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = {
                        whRatio = 4f / 3f
                    }) {
                        Text("4:3")
                    }
                    Button(onClick = {
                        whRatio = 9f / 16f
                    }) {
                        Text("9:16")
                    }
                    Button(onClick = {
                        whRatio = 1f
                    }) {
                        Text("1:1")
                    }
                    Button(onClick = { whRatio = 3f / 4f }) {
                        Text("Default")
                    }
                }
            }
        }
    }
}