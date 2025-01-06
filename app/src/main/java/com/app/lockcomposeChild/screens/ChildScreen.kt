package com.app.lockcomposeChild.screens

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import com.app.lockcomposeChild.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream



@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildScreen(navController: NavController) {
    val context = LocalContext.current
    var profileType by remember { mutableStateOf("No Selected Profile") }
    var installedApps by remember { mutableStateOf(listOf<InstalledApp>()) }
    val showQRCode = remember { mutableStateOf(false) }
    val qrData = remember { "${getDeviceName()},${generateDeviceID(context)}" }
    val qrCodeBitmap = remember { generateQRCodeForData(qrData) }


    LaunchedEffect(Unit) {

        val firebaseDatabase = FirebaseDatabase.getInstance().getReference()
            .child("Apps").child(generateDeviceID(context))

        firebaseDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                profileType = snapshot.child("type").getValue(String::class.java) ?: "No Selected Profile"
                if(profileType == "Custom"){
                    uploadInstalledAppsOnStart(context)
                    navController.navigate("custom")
                } else {
                    installedApps = getAppsForProfile(context, profileType)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to fetch profile: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    if (showQRCode.value && qrCodeBitmap != null) {
        Dialog(onDismissRequest = { showQRCode.value = false }) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        bitmap = qrCodeBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        showQRCode.value = false
                    }) {
                        Text("Close")
                    }
                }
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = { Text(profileType, color = Color.Black) },
            actions = {
                IconButton(onClick = { showQRCode.value = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.qr_code),
                        contentDescription = "Generate QR Code",
                        tint = Color.Black
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.LightGray)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(installedApps) { app ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Image(
                            painter = BitmapPainter(app.icon.toBitmap().asImageBitmap()),
                            contentDescription = app.name,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = app.name,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        Button(
            onClick = {
                uploadToFirebase(installedApps, profileType)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text("Submit")
        }
    }
}

fun getDeviceName(): String = Build.MODEL ?: "Unknown Device"

fun getAppsForProfile(context: Context, profileType: String): List<InstalledApp> {
    val appsForProfile = profileApps[profileType] ?: return emptyList()
    val installedApps = getInstalledApps(context)
    return installedApps.filter { app -> appsForProfile.contains(app.packageName) }
}
fun uploadToFirebase(appsList: List<InstalledApp>, profileType: String) {
    val firebaseDatabase = FirebaseDatabase.getInstance().reference.child("childApp")
    firebaseDatabase.removeValue()
    appsList.forEach { app ->
        val appData = mapOf(
            "name" to app.name,
            "pin_code" to "0",
            "interval" to "0",
            "package_name" to app.packageName,
            "icon" to bitmapToBase64(app.icon.toBitmap()),
            "profile_type" to profileType
        )

        firebaseDatabase.child(app.name).setValue(appData).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("Firebase", "Data uploaded successfully")
            } else {
                Log.e("Firebase", "Data upload failed", it.exception)
            }
        }
    }
}

fun bitmapToBase64(bitmap: Bitmap): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
    val byteArray = byteArrayOutputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}

fun drawableToBitmap(drawable: Drawable): Bitmap? {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }
    val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

fun getInstalledApps(context: Context): List<InstalledApp> {
    val packageManager = context.packageManager
    val installedPackages = packageManager.getInstalledPackages(0)
    return installedPackages.mapNotNull { packageInfo ->
        val isSystemApp = packageInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
        if (!isSystemApp) {
            val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
            val packageName = packageInfo.packageName
            val appIcon = packageInfo.applicationInfo.loadIcon(packageManager)
            InstalledApp(appName, packageName, appIcon)
        } else null
    }
}

fun uploadInstalledAppsOnStart(context: Context) {

    val installedApps = getInstalledApps(context)
    val firebaseDatabase = FirebaseDatabase.getInstance().reference.child("applications")
        .child(generateDeviceID(context))

    installedApps.forEach { app ->

        val appData = mapOf(
            "name" to app.name,
            "package_name" to app.packageName,
            "icon" to bitmapToBase64(drawableToBitmap(app.icon)!!),
            "profile_type" to "Custom"
        )
        firebaseDatabase.child(app.name).setValue(appData).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("Firebase", "Installed app uploaded successfully")
            } else {
                Log.e("Firebase", "Failed to upload installed app", it.exception)
            }
        }
    }
}


@SuppressLint("HardwareIds")
fun generateDeviceID(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}

data class InstalledApp(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

val profileApps = mapOf(
    "Child" to listOf(
        "com.android.gallery",
        "com.android.gallery3d",
        "com.android.dialer",
        "com.google.android.apps.youtube.kids",
        "org.khanacademy.android",
        "com.duolingo"
    ),
    "Teen" to listOf(
        "com.android.messaging",
        "com.samsung.android.messaging",
        "com.android.chrome",
        "com.facebook.katana",
        "com.instagram.android",
        "com.google.android.youtube"
    ),
    "Pre-K" to listOf(
        "com.google.android.apps.maps",
        "com.android.camera",
        "com.sec.android.app.camera",
        "com.google.android.GoogleCamera",
        "com.android.chrome",
        "com.google.android.apps.youtube.kids",
        "com.facebook.katana"
    )
)


fun generateQRCodeForData(data: String): Bitmap? {
    val width = 300
    val height = 300
    val qrCodeWriter = com.google.zxing.qrcode.QRCodeWriter()
    try {
        val bitMatrix = qrCodeWriter.encode(data, com.google.zxing.BarcodeFormat.QR_CODE, width, height)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        return bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
