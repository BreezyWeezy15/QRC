import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.ParcelUuid
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.widget.Button

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.app.lockcomposeChild.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowAppList() {
    val context = LocalContext.current
    val appsList = remember { mutableStateOf<List<InstalledApps>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val showToast = remember { mutableStateOf(false) }
    val showQRCode = remember { mutableStateOf(false) }

    val qrData = remember { "${getDeviceName()},${generateDeviceID(context)}" }
    val qrCodeBitmap = remember { generateQRCodeForData(qrData) }



    DisposableEffect(Unit) {

        isLoading.value = false

        val firebaseDatabase = FirebaseDatabase.getInstance().reference.child("Apps")
            .child(generateDeviceID(context))

        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val updatedList = mutableListOf<InstalledApps>()

                for (childSnapshot in dataSnapshot.children) {
                    val packageName = childSnapshot.child("package_name").getValue(String::class.java) ?: ""
                    val name = childSnapshot.child("name").getValue(String::class.java) ?: ""
                    val base64Icon = childSnapshot.child("icon").getValue(String::class.java) ?: ""
                    val interval = childSnapshot.child("interval").getValue(String::class.java) ?: ""
                    val pinCode = childSnapshot.child("pin_code").getValue(String::class.java) ?: ""

                    val iconBitmap = base64ToBitmap(base64Icon)

                    val installedApp = iconBitmap?.let {
                        InstalledApps(
                            packageName = packageName,
                            name = name,
                            icon = it,
                            interval = interval,
                            pinCode = pinCode
                        )
                    }

                    if (installedApp != null) {
                        updatedList.add(installedApp)
                    }
                }
                appsList.value = updatedList
                isLoading.value = false
            }

            override fun onCancelled(databaseError: DatabaseError) {
                isLoading.value = false
            }
        }

        firebaseDatabase.addValueEventListener(valueEventListener)
        onDispose {
            firebaseDatabase.removeEventListener(valueEventListener)
        }
    }

    if (showToast.value) {
        Toast.makeText(context, "Data sent successfully", Toast.LENGTH_SHORT).show()
        showToast.value = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Child App", color = Color.Black) },
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
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp)  // Ensure space for button
                ) {
                    if (isLoading.value) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(appsList.value) { app ->
                                AppListItem(
                                    app = app,
                                    interval = app.interval,
                                    pinCode = app.pinCode
                                )
                            }
                        }
                    }
                }
                Button(
                    onClick = { uploadToFirebase(appsList.value) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .align(Alignment.BottomCenter),
                    shape = RectangleShape, // No corners
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Submit")
                }
            }
        }
    )

    if (showQRCode.value && qrCodeBitmap != null) {
        Dialog(onDismissRequest = {
            uploadInstalledAppsOnStartup(context)
            showQRCode.value = false
        }) {
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
                        uploadInstalledAppsOnStartup(context)
                        showQRCode.value = false
                    }) {
                        Text("Close")
                    }
                }
            }
        }
    }
}


@Composable
fun AppListItem(app: InstalledApps, interval: String, pinCode: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RectangleShape
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap = app.icon.asImageBitmap(),
                    contentDescription = app.name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "Interval: $interval Min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pin Code: $pinCode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Utility functions
@SuppressLint("HardwareIds")
fun generateDeviceID(context: Context): String {
    return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}

fun getDeviceName(): String = Build.MODEL ?: "Unknown Device"

fun getInstalledApps(context: Context): List<InstalledApps> {
    val packageManager = context.packageManager
    val installedPackages = packageManager.getInstalledPackages(0)
    return installedPackages.mapNotNull { packageInfo ->
        val isSystemApp = packageInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
        if (!isSystemApp) {
            val appName = packageInfo.applicationInfo.loadLabel(packageManager).toString()
            val packageName = packageInfo.packageName
            val appIcon = drawableToBitmap(packageInfo.applicationInfo.loadIcon(packageManager))
            appIcon?.let { InstalledApps(packageName, appName, it, "0", "0000") }
        } else null
    }
}

fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap? {
    return if (drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0) {
        Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888).apply {
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            drawable.draw(android.graphics.Canvas(this))
        }
    } else null
}

fun base64ToBitmap(base64Str: String): Bitmap? {
    return try {
        val decodedString = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
    } catch (e: IllegalArgumentException) {
        null
    }
}

fun generateQRCodeForData(data: String): Bitmap? {
    return try {
        val qrCodeWriter = com.google.zxing.qrcode.QRCodeWriter()
        val hints = mapOf(com.google.zxing.EncodeHintType.MARGIN to 1)
        val bitMatrix = qrCodeWriter.encode(data, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512, hints)
        Bitmap.createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565).apply {
            for (x in 0 until bitMatrix.width) {
                for (y in 0 until bitMatrix.height) {
                    setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
        }
    } catch (e: Exception) {
        null
    }
}

data class InstalledApps(
    val packageName: String,
    val name: String,
    val icon: Bitmap,
    val interval: String,
    val pinCode: String
)

fun uploadInstalledAppsOnStartup(context: Context) {

    val installedApps = getInstalledApps(context)
    val firebaseDatabase = FirebaseDatabase.getInstance().reference.child("applications")
        .child(generateDeviceID(context))

    installedApps.forEach { app ->

        val appData = mapOf(
            "name" to app.name,
            "package_name" to app.packageName,
            "icon" to bitmapToBase64(app.icon),
            "interval" to app.interval,
            "pin_code" to app.pinCode
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



fun uploadToFirebase(appsList: List<InstalledApps>) {
    val firebaseDatabase = FirebaseDatabase.getInstance().reference.child("childApp")

    appsList.forEach { app ->
        val appData = mapOf(
            "name" to app.name,
            "package_name" to app.packageName,
            "icon" to bitmapToBase64(app.icon),
            "interval" to app.interval,
            "pin_code" to app.pinCode
        )

        firebaseDatabase.push().setValue(appData).addOnCompleteListener {
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
