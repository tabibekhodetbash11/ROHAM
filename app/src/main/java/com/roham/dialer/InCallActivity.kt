package com.roham.dialer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.net.Uri

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { InCallScreen() }
    }

    override fun onResume() {
        super.onResume()
        RohamInCallService.refreshCallState()
    }
}

class RohamInCallService : InCallService() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        call.registerCallback(callCallback)
        refreshCallState()
        startActivity(
            Intent(this, InCallActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(callCallback)
        if (currentCall === call) currentCall = calls.lastOrNull()
        refreshCallState()
        super.onCallRemoved(call)
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) = refreshCallState()
        override fun onDetailsChanged(call: Call, details: Call.Details) = refreshCallState()
    }

    companion object {
        var instance: RohamInCallService? = null
        var currentCall: Call? = null
        var callState by mutableIntStateOf(Call.STATE_DISCONNECTED)
            private set
        var muted by mutableStateOf(false)
        var speaker by mutableStateOf(false)

        fun refreshCallState() {
            callState = currentCall?.state ?: Call.STATE_DISCONNECTED
        }

        fun toggleMute() {
            muted = !muted
            instance?.setMuted(muted)
        }

        fun toggleSpeaker() {
            speaker = !speaker
            instance?.setAudioRoute(
                if (speaker) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE
            )
        }

        fun toggleHold() {
            val call = currentCall ?: return
            if (call.state == Call.STATE_HOLDING) call.unhold() else if (call.state == Call.STATE_ACTIVE) call.hold()
        }
    }
}

@Composable
private fun InCallScreen() {
    val call = RohamInCallService.currentCall
    val state = RohamInCallService.callState
    val muted = RohamInCallService.muted
    val speaker = RohamInCallService.speaker
    val raw = call?.details?.handle?.schemeSpecificPart.orEmpty()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val contact = remember(raw) { resolveName(ctx, raw) }
    val incoming = state == Call.STATE_RINGING
    val held = state == Call.STATE_HOLDING

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF11162B), Color(0xFF050711))))
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ROHAM", color = Color(0xFF35E7FF), fontSize = 30.sp, fontWeight = FontWeight.Black)
            Text(if (incoming) "تماس ورودی" else if (held) "تماس در انتظار" else "تماس", color = Color(0xFFB45CFF), fontSize = 14.sp)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(contact ?: "شماره ناشناس", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(if (contact == null) raw.ifBlank { "شماره نمایش داده نمی‌شود" } else "مخاطب ذخیره‌شده", color = Color.LightGray, fontSize = 14.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            CallControl(if (muted) Icons.Default.MicOff else Icons.Default.Mic, if (muted) "وصل میکروفن" else "بی‌صدا", muted) {
                RohamInCallService.toggleMute()
            }
            CallControl(Icons.Default.VolumeUp, "بلندگو", speaker) {
                RohamInCallService.toggleSpeaker()
            }
            CallControl(if (held) Icons.Default.PlayArrow else Icons.Default.Pause, if (held) "ادامه" else "انتظار", held) {
                RohamInCallService.toggleHold()
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            if (incoming) {
                RoundAction(Color(0xFF22C98A), Icons.Default.Call, "پاسخ") {
                    call?.answer(VideoProfile.STATE_AUDIO_ONLY)
                }
                RoundAction(Color(0xFFFF3F70), Icons.Default.CallEnd, "رد تماس") {
                    call?.disconnect()
                    (ctx as? ComponentActivity)?.finish()
                }
            } else {
                RoundAction(Color(0xFFFF3F70), Icons.Default.CallEnd, "پایان تماس") {
                    call?.disconnect()
                    (ctx as? ComponentActivity)?.finish()
                }
            }
        }
    }
}

@Composable
private fun CallControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(62.dp)
                .background(if (selected) Color(0xFF174C5B) else Color(0xFF202941), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = Color(0xFF35E7FF), modifier = Modifier.size(26.dp))
        }
        Text(label, color = Color.White, fontSize = 11.sp)
    }
}

@Composable
private fun RoundAction(
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(76.dp).background(color, CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

private fun resolveName(context: Context, number: String): String? {
    if (number.isBlank() || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
    val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
    context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) return it.getString(0)
    }
    return null
}
