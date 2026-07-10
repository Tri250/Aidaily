package com.livecomposepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphicspackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
importpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
importpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.composepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animationpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidxpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
importpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interactionpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.Lpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidxpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filledpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidxpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidxpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
importpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidxpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.inputpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.composepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.textpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidxpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import compackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.Dngpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResultpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.corepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.Compackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
importpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.corepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.Bepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.Spackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import compackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.Videopackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.featurespackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
importpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecordpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapturepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinxpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapturepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatcherspackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditorpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContextpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutablepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0fpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifierpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Rowpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Centerpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Textpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Textpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabelpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.Semipackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming =package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLongpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLongpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        ifpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimCompletepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败",package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${epackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
                            }
                        }
                    },
                    enabled = !isTrimming
                ) {
                    Text(
                        if (ispackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
                            }
                        }
                    },
                    enabled = !isTrimming
                ) {
                    Text(
                        if (isTrimming) "处理中..." else "裁剪",
                        color = if (isTrimpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
                            }
                        }
                    },
                    enabled = !isTrimming
                ) {
                    Text(
                        if (isTrimming) "处理中..." else "裁剪",
                        color = if (isTrimming) DesignSystem.Colors.minimalLabelTertiary else DesignSystem.Colors.primary
                    )
                }
            }

package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
                            }
                        }
                    },
                    enabled = !isTrimming
                ) {
                    Text(
                        if (isTrimming) "处理中..." else "裁剪",
                        color = if (isTrimming) DesignSystem.Colors.minimalLabelTertiary else DesignSystem.Colors.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
                            }
                        }
                    },
                    enabled = !isTrimming
                ) {
                    Text(
                        if (isTrimming) "处理中..." else "裁剪",
                        color = if (isTrimming) DesignSystem.Colors.minimalLabelTertiary else DesignSystem.Colors.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 视频预览占位（实际应使用 VideoView）
            Box(
package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
                            }
                        }
                    },
                    enabled = !isTrimming
                ) {
                    Text(
                        if (isTrimming) "处理中..." else "裁剪",
                        color = if (isTrimming) DesignSystem.Colors.minimalLabelTertiary else DesignSystem.Colors.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 视频预览占位（实际应使用 VideoView）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
                            }
                        }
                    },
                    enabled = !isTrimming
                ) {
                    Text(
                        if (isTrimming) "处理中..." else "裁剪",
                        color = if (isTrimming) DesignSystem.Colors.minimalLabelTertiary else DesignSystem.Colors.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 视频预览占位（实际应使用 VideoView）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .background(package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
                            }
                        }
                    },
                    enabled = !isTrimming
                ) {
                    Text(
                        if (isTrimming) "处理中..." else "裁剪",
                        color = if (isTrimming) DesignSystem.Colors.minimalLabelTertiary else DesignSystem.Colors.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 视频预览占位（实际应使用 VideoView）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .background(DesignSystem.Colors.minimalSurface),
                contentAlignment = Alignment.Center
            ) {
package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
                            }
                        }
                    },
                    enabled = !isTrimming
                ) {
                    Text(
                        if (isTrimming) "处理中..." else "裁剪",
                        color = if (isTrimming) DesignSystem.Colors.minimalLabelTertiary else DesignSystem.Colors.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 视频预览占位（实际应使用 VideoView）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .background(DesignSystem.Colors.minimalSurface),
                contentAlignment = Alignment.Center
            ) {
                // 尝试加载缩略图
                var thumbnail by remember(videoPath) { mutableStatepackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
                            }
                        }
                    },
                    enabled = !isTrimming
                ) {
                    Text(
                        if (isTrimming) "处理中..." else "裁剪",
                        color = if (isTrimming) DesignSystem.Colors.minimalLabelTertiary else DesignSystem.Colors.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 视频预览占位（实际应使用 VideoView）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .background(DesignSystem.Colors.minimalSurface),
                contentAlignment = Alignment.Center
            ) {
                // 尝试加载缩略图
                var thumbnail by remember(videoPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
                LaunchedEffect(videoPath) {
package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
                            }
                        }
                    },
                    enabled = !isTrimming
                ) {
                    Text(
                        if (isTrimming) "处理中..." else "裁剪",
                        color = if (isTrimming) DesignSystem.Colors.minimalLabelTertiary else DesignSystem.Colors.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 视频预览占位（实际应使用 VideoView）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .background(DesignSystem.Colors.minimalSurface),
                contentAlignment = Alignment.Center
            ) {
                // 尝试加载缩略图
                var thumbnail by remember(videoPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
                LaunchedEffect(videoPath) {
                    thumbnail = try {
                        videoEditor.generateThumbnail(videoPath)
                    } catch (epackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
                            }
                        }
                    },
                    enabled = !isTrimming
                ) {
                    Text(
                        if (isTrimming) "处理中..." else "裁剪",
                        color = if (isTrimming) DesignSystem.Colors.minimalLabelTertiary else DesignSystem.Colors.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 视频预览占位（实际应使用 VideoView）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .background(DesignSystem.Colors.minimalSurface),
                contentAlignment = Alignment.Center
            ) {
                // 尝试加载缩略图
                var thumbnail by remember(videoPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
                LaunchedEffect(videoPath) {
                    thumbnail = try {
                        videoEditor.generateThumbnail(videoPath)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (thumbnail !=package com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
                            }
                        }
                    },
                    enabled = !isTrimming
                ) {
                    Text(
                        if (isTrimming) "处理中..." else "裁剪",
                        color = if (isTrimming) DesignSystem.Colors.minimalLabelTertiary else DesignSystem.Colors.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 视频预览占位（实际应使用 VideoView）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .background(DesignSystem.Colors.minimalSurface),
                contentAlignment = Alignment.Center
            ) {
                // 尝试加载缩略图
                var thumbnail by remember(videoPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
                LaunchedEffect(videoPath) {
                    thumbnail = try {
                        videoEditor.generateThumbnail(videoPath)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentpackage com.livecompose.livecapture.features.capture.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livecompose.livecapture.core.camera.*
import com.livecompose.livecapture.core.camera.RawCaptureManager
import com.livecompose.livecapture.core.camera.DngCaptureManager
import com.livecompose.livecapture.core.camera.HyperfocalCalculator
import com.livecompose.livecapture.core.camera.HyperfocalResult
import com.livecompose.livecapture.core.filter.AiFilterRecommender
import com.livecompose.livecapture.core.filter.FilterRecommendation
import com.livecompose.livecapture.core.composition.ARCompositionGuideOverlay
import com.livecompose.livecapture.core.composition.CompositionScorer
import com.livecompose.livecapture.core.composition.CompositionGuideType
import com.livecompose.livecapture.core.composition.CompositionScore
import com.livecompose.livecapture.core.onboarding.FeatureTipOverlay
import com.livecompose.livecapture.core.permission.PermissionManager
import com.livecompose.livecapture.core.performance.MemoryUsageView
import com.livecompose.livecapture.core.processing.QuickShotManager
import com.livecompose.livecapture.core.processing.MultipleExposure
import com.livecompose.livecapture.core.portrait.PortraitViewModel
import com.livecompose.livecapture.core.portrait.BeautyPreset as PortraitBeautyPreset
import com.livecompose.livecapture.core.video.VideoViewModel
import com.livecompose.livecapture.core.video.VideoEditor
import com.livecompose.livecapture.core.video.SlowMotionRecorder
import com.livecompose.livecapture.core.video.VideoStabilizer
import com.livecompose.livecapture.core.video.VideoMode
import com.livecompose.livecapture.core.video.SlowMotionSpeed
import com.livecompose.livecapture.core.video.VideoRecordingState
import com.livecompose.livecapture.di.AppContainer
import com.livecompose.livecapture.features.capture.PhotoCaptureResult
import com.livecompose.livecapture.features.home.HomeViewModel
import com.livecompose.livecapture.core.storage.PhotoRecord
import com.livecompose.livecapture.core.lut.MasterPreset
import com.livecompose.livecapture.ui.design.DesignSystem
import com.livecompose.livecapture.ui.design.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.livecompose.livecapture.utilities.HapticManager
import com.livecompose.livecapture.features.profile.ProfileScreen
import com.livecompose.livecapture.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ====== 视频编辑浮层 ======

@Composable
internal fun VideoEditorOverlay(
    videoPath: String,
    videoEditor: VideoEditor,
    startTimeUs: Long,
    endTimeUs: Long,
    onDismiss: () -> Unit,
    onTrimComplete: (String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isTrimming by remember { mutableStateOf(false) }
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }
    val totalDuration = endTimeUs - startTimeUs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DesignSystem.Colors.minimalBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = DesignSystem.Colors.minimalLabelSecondary)
                }
                Text(
                    "编辑视频",
                    style = DesignSystem.Typography.title3,
                    color = DesignSystem.Colors.minimalLabel,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(
                    onClick = {
                        if (!isTrimming) {
                            isTrimming = true
                            val startTime = startTimeUs + (totalDuration * trimStart).toLong()
                            val endTime = startTimeUs + (totalDuration * trimEnd).toLong()
                            scope.launch {
                                try {
                                    videoEditor.trimVideo(videoPath, startTime, endTime) { outputPath ->
                                        isTrimming = false
                                        if (outputPath != null) {
                                            Toast.makeText(context, "视频裁剪完成", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(outputPath)
                                        } else {
                                            Toast.makeText(context, "视频裁剪失败", Toast.LENGTH_SHORT).show()
                                            onTrimComplete(null)
                                        }
                                    }
                                } catch (e: Exception) {
                                    isTrimming = false
                                    Toast.makeText(context, "视频编辑失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    onTrimComplete(null)
                                }
                            }
                        }
                    },
                    enabled = !isTrimming
                ) {
                    Text(
                        if (isTrimming) "处理中..." else "裁剪",
                        color = if (isTrimming) DesignSystem.Colors.minimalLabelTertiary else DesignSystem.Colors.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 视频预览占位（实际应使用 VideoView）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(DesignSystem.CornerRadius.large))
                    .background(DesignSystem.Colors.minimalSurface),
                contentAlignment = Alignment.Center
            ) {
                // 尝试加载缩略图
                var thumbnail by remember(videoPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
                LaunchedEffect(videoPath) {
                    thumbnail = try {
                        videoEditor.generateThumbnail(videoPath)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail!!.asImageBitmap(),
                        contentDescription = "视频预览",
                        modifier =