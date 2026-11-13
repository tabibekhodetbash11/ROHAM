package com.roham.dialer

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.CallLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val Bg = Color(0xFF080B16)
private val Panel = Color(0xFF11182A)
private val Cyan = Color(0xFF35E7FF)
private val Purple = Color(0xFFB45CFF)
private val Pink = Color(0xFFFF4FCB)

data class Person(val name: String, val number: String)
data class RecentCall(val title: String, val number: String, val type: Int, val date: Long)

class MainActivity : ComponentActivity() {
    private var contactsGranted by mutableStateOf(false)
    private var callLogGranted by mutableStateOf(false)
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        contactsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        callLogGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
    }
    private val role = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        callLogGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contactsGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        callLogGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        if (!contactsGranted) permissions.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.ANSWER_PHONE_CALLS))
        setContent {
            RohamTheme {
                var tab by remember { mutableStateOf(0) }
                var dial by remember { mutableStateOf("") }
                var query by remember { mutableStateOf("") }
                val people = remember(contactsGranted) { if (contactsGranted) loadContacts(this@MainActivity) else emptyList() }
                val recentCalls = remember(callLogGranted) { if (callLogGranted) loadRecentCalls(this@MainActivity) else emptyList() }
                Column(Modifier.fillMaxSize().background(Bg).padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ROHAM", color = Cyan, fontSize = 30.sp, fontWeight = FontWeight.Black)
                            Text("NEON SMART DIALER", color = Purple, fontSize = 10.sp, letterSpacing = 3.sp)
                        }
                        IconButton(onClick = { requestDialerRole() }) { Icon(Icons.Default.Settings, null, tint = Cyan) }
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(18.dp)).padding(5.dp)) {
                        listOf("شماره‌گیر", "مخاطبین", "سوابق").forEachIndexed { i, label ->
                            Box(Modifier.weight(1f).background(if (tab == i) Brush.horizontalGradient(listOf(Cyan.copy(.25f), Purple.copy(.25f))) else Brush.horizontalGradient(listOf(Panel, Panel)), RoundedCornerShape(14.dp)).clickable { tab = i }.padding(12.dp), contentAlignment = Alignment.Center) {
                                Text(label, color = if (tab == i) Cyan else Color.LightGray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    if (tab == 0) {
                        Text("شماره‌گیری سریع", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(14.dp))
                        Box(Modifier.fillMaxWidth().height(64.dp).border(1.dp, Cyan.copy(.35f), RoundedCornerShape(16.dp)).background(Panel, RoundedCornerShape(16.dp)).padding(14.dp), contentAlignment = Alignment.Center) {
                            Text(if (dial.isBlank()) "شماره را وارد کنید" else dial, color = Color.White, fontSize = 24.sp)
                        }
                        Spacer(Modifier.height(18.dp))
                        val keys = listOf("1","2","3","4","5","6","7","8","9","*","0","#")
                        for (r in 0..3) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                for (c in 0..2) {
                                    val key = keys[r * 3 + c]
                                    Box(Modifier.weight(1f).aspectRatio(1f).shadow(10.dp, CircleShape, ambientColor = Purple.copy(.2f), spotColor = Cyan.copy(.25f)).border(1.dp, Brush.linearGradient(listOf(Cyan.copy(.75f), Purple.copy(.75f))), CircleShape).background(Panel, CircleShape).clickable { dial += key }, contentAlignment = Alignment.Center) {
                                        Text(key, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (dial.isNotEmpty()) dial = dial.dropLast(1) }) { Icon(Icons.Default.Backspace, null, tint = Pink) }
                            Spacer(Modifier.width(24.dp))
                            Box(Modifier.size(68.dp).shadow(14.dp, CircleShape, spotColor = Cyan).background(Brush.linearGradient(listOf(Cyan, Color(0xFF00A88F))), CircleShape).clickable { placeCall(dial) }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Call, null, tint = Bg, modifier = Modifier.size(30.dp))
                            }
                            Spacer(Modifier.width(24.dp))
                            IconButton(onClick = { dial = "" }) { Icon(Icons.Default.Close, null, tint = Color.LightGray) }
                        }
                    } else if (tab == 1) {
                        Text("مخاطبین شما", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        BasicTextField(query, { query = it }, Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(16.dp), textStyle = TextStyle(color = Color.White, fontSize = 16.sp), decorationBox = { inner -> if (query.isEmpty()) Text("جستجوی نام مخاطب", color = Color.Gray); inner() })
                        Spacer(Modifier.height(12.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            items(people.filter { it.name.contains(query, true) }) { person ->
                                Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(15.dp)).clickable { placeCall(person.number) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(42.dp).background(Brush.linearGradient(listOf(Cyan.copy(.3f), Purple.copy(.35f))), CircleShape), contentAlignment = Alignment.Center) { Text(person.name.take(1), color = Cyan, fontWeight = FontWeight.Bold) }
                                    Spacer(Modifier.width(12.dp))
                                    Text(person.name, color = Color.White, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.Call, null, tint = Cyan)
                                }
                            }
                        }
                    } else {
                        Text("تماس‌های اخیر", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        if (!callLogGranted) {
                            Text("برای نمایش سوابق، ROHAM را به‌عنوان شماره‌گیر پیش‌فرض انتخاب کنید و مجوز سوابق تماس را فعال کنید.", color = Color.LightGray, fontSize = 13.sp)
                        } else if (recentCalls.isEmpty()) {
                            Text("هنوز سابقه‌ای برای نمایش وجود ندارد.", color = Color.Gray, fontSize = 14.sp)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                items(recentCalls) { item ->
                                    Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(15.dp)).clickable { dial = item.number; tab = 0 }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            Text(callTypeLabel(item.type) + "  •  " + SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault()).format(Date(item.date)), color = Color.Gray, fontSize = 11.sp)
                                        }
                                        Icon(Icons.Default.Call, null, tint = Cyan)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("طراحی شده توسط رهام پارسایی  •  NEON EDITION", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp))
                }
            }
        }
    }

    private fun requestDialerRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(RoleManager::class.java)
            if (rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) role.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        } else {
            val tm = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            startActivity(Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName))
        }
    }

    private fun placeCall(number: String) {
        if (number.isBlank()) return
        val uri = Uri.fromParts("tel", number, null)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) startActivity(Intent(Intent.ACTION_CALL, uri))
        else startActivity(Intent(Intent.ACTION_DIAL, uri))
    }
}

private fun loadContacts(context: Context): List<Person> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return emptyList()
    val result = mutableListOf<Person>()
    val cursor = context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC")
    cursor?.use {
        while (it.moveToNext()) {
            val name = it.getString(0) ?: "بدون نام"
            val number = it.getString(1) ?: ""
            if (result.none { p -> p.name == name && p.number == number }) result.add(Person(name, number))
        }
    }
    return result
}

@Composable
private fun RohamTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Panel, primary = Cyan), content = content)
}


private fun loadRecentCalls(context: Context): List<RecentCall> {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) return emptyList()
    val rows = mutableListOf<RecentCall>()
    val projection = arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE)
    context.contentResolver.query(CallLog.Calls.CONTENT_URI, projection, null, null, CallLog.Calls.DATE + " DESC")?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
        val numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)
        val typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE)
        val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)
        while (cursor.moveToNext() && rows.size < 50) {
            val name = cursor.getString(nameIndex).orEmpty()
            val number = cursor.getString(numberIndex).orEmpty()
            rows.add(RecentCall(name.ifBlank { number.ifBlank { "شماره ناشناس" } }, number, cursor.getInt(typeIndex), cursor.getLong(dateIndex)))
        }
    }
    return rows
}

private fun callTypeLabel(type: Int): String = when (type) {
    CallLog.Calls.INCOMING_TYPE -> "تماس دریافتی"
    CallLog.Calls.OUTGOING_TYPE -> "تماس خروجی"
    CallLog.Calls.MISSED_TYPE -> "تماس ازدست‌رفته"
    CallLog.Calls.REJECTED_TYPE -> "تماس ردشده"
    else -> "تماس"
}
