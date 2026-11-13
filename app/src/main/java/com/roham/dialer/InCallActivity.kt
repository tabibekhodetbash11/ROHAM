package com.roham.dialer

import android.os.Bundle
import android.telecom.Call
import android.telecom.InCallService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { InCallScreen() }
    }
}

class RohamInCallService : InCallService() {
    override fun onCreate() { super.onCreate(); instance = this }
    override fun onDestroy() { instance = null; super.onDestroy() }
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        startActivity(android.content.Intent(this, InCallActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    override fun onCallRemoved(call: Call) { if (currentCall === call) currentCall = null; super.onCallRemoved(call) }
    companion object {
        var currentCall: Call? = null
        var instance: RohamInCallService? = null
        var speaker = false
    }
}

@Composable
private fun InCallScreen() {
    val call = RohamInCallService.currentCall
    val details = call?.details
    val raw = details?.handle?.schemeSpecificPart.orEmpty()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val contact = remember(raw) { resolveName(ctx, raw) }
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF11162B), Color(0xFF050711)))).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
        Text("ROHAM", color = Color(0xFF35E7FF), fontSize = 30.sp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(contact ?: "تماس", color = Color.White, fontSize = 28.sp)
            Text(if (contact == null) "شماره ناشناس" else "مخاطب ذخیره‌شده", color = Color(0xFFB45CFF), fontSize = 14.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(30.dp)) {
            androidx.compose.foundation.layout.Box(Modifier.size(64.dp).background(Color(0xFF202941), CircleShape).clickable { call?.hold() }, contentAlignment = Alignment.Center) { Icon(Icons.Default.MicOff, null, tint = Color(0xFF35E7FF)) }
            androidx.compose.foundation.layout.Box(Modifier.size(64.dp).background(Color(0xFF202941), CircleShape).clickable {
                RohamInCallService.speaker = !RohamInCallService.speaker
                RohamInCallService.instance?.setAudioRoute(if (RohamInCallService.speaker) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
            }, contentAlignment = Alignment.Center) { Icon(Icons.Default.VolumeUp, null, tint = Color(0xFF35E7FF)) }
            androidx.compose.foundation.layout.Box(Modifier.size(64.dp).background(Color(0xFF202941), CircleShape).clickable { call?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY) }, contentAlignment = Alignment.Center) { Icon(Icons.Default.Call, null, tint = Color(0xFF35E7FF)) }
        }
        androidx.compose.foundation.layout.Box(Modifier.size(78.dp).background(Color(0xFFFF3F70), CircleShape).clickable { call?.disconnect(); (ctx as? ComponentActivity)?.finish() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
    }
}

private fun resolveName(context: android.content.Context, number: String): String? {
    if (number.isBlank()) return null
    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return null
    val uri = android.net.Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(number))
    context.contentResolver.query(uri, arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) return it.getString(0)
    }
    return null
}
