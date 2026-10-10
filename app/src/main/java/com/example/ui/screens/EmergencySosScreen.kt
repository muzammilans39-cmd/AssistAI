package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.EmergencyContactEntity
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.EmeraldSuccess

@Composable
fun EmergencySosScreen(
    contacts: List<EmergencyContactEntity>,
    onTriggerAudibleSos: () -> Unit,
    onAddContact: (String, String, String, String) -> Unit,
    onDeleteContact: (EmergencyContactEntity) -> Unit,
    onSpeakText: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var showAddContact by rememberSaveable { mutableStateOf(false) }
    var contactName by rememberSaveable { mutableStateOf("") }
    var contactRel by rememberSaveable { mutableStateOf("Family Member") }
    var contactPhone by rememberSaveable { mutableStateOf("") }
    var contactNotes by rememberSaveable { mutableStateOf("") }

    val medicalIdSummary = "Medical ID for Arthur Miller (Age 74). Blood Type: O Positive. Allergies: Penicillin (Severe), Sulfa drugs. Medical Conditions: Hypertension, Type 2 Diabetes, Mild Low Vision. Daily Medications: Amlodipine 5mg, Metformin 500mg."

    var pendingCallPhone by remember { mutableStateOf<String?>(null) }

    fun executeImmediateCallOrDial(phone: String) {
        val cleanPhone = phone.filter { it.isDigit() || it == '+' || it == '-' }.ifBlank { "555-0911" }
        val uri = Uri.parse("tel:$cleanPhone")
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCallPermission) {
            try {
                context.startActivity(Intent(Intent.ACTION_CALL, uri))
                return
            } catch (_: Exception) {
            }
        }
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, uri))
        } catch (_: Exception) {
        }
    }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val phone = pendingCallPhone ?: "555-0911"
        pendingCallPhone = null
        executeImmediateCallOrDial(phone)
    }

    fun dialPhoneNumber(phone: String) {
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        if (hasCallPermission) {
            executeImmediateCallOrDial(phone)
        } else {
            pendingCallPhone = phone
            try {
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            } catch (_: Exception) {
                executeImmediateCallOrDial(phone)
            }
        }
    }

    fun shareEmergencyStatus() {
        try {
            val primary = contacts.firstOrNull()
            val shareText = "🆘 AssistAI Emergency & Caregiver Check-In:\n$medicalIdSummary\nPrimary Contact: ${primary?.name ?: "Sarah Miller"} (${primary?.phoneNumber ?: "555-0911"})."
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "AssistAI Emergency Medical & Caregiver Summary")
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Share Emergency Summary"))
        } catch (_: Exception) {
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("emergency_sos_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Huge Audible SOS & Medical Beacon Card
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                border = BorderStroke(3.dp, AlertCrimson),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🆘 EMERGENCY VOICE BEACON & SOS",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap the large red button below to broadcast your Medical ID aloud and alert nearby helpers or your primary caregiver.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Button(
                        onClick = onTriggerAudibleSos,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 84.dp)
                            .testTag("trigger_sos_beacon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Emergency,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ACTIVATE SOS VOICE BEACON",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. High-Contrast Emergency Medical ID Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("medical_id_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MedicalInformation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "EMERGENCY MEDICAL ID",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { onSpeakText(medicalIdSummary) }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Read Medical ID aloud"
                            )
                        }
                    }

                    Text("👤 Name: Arthur Miller (Age 74)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("🩸 Blood Type: O Positive (O+)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("⚠️ Allergies: Penicillin (Severe), Sulfa Drugs", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    Text("🩺 Conditions: Hypertension, Type 2 Diabetes, Low Vision", style = MaterialTheme.typography.bodyLarge)
                    Text("💊 Active Meds: Amlodipine 5mg, Metformin 500mg", style = MaterialTheme.typography.bodyLarge)

                    OutlinedButton(
                        onClick = { shareEmergencyStatus() },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("share_emergency_summary_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share Medical ID & Status with Caregiver", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        // 3. Trusted Caregiver & Emergency Quick-Dial Contacts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CAREGIVER & QUICK-DIAL CONTACTS (${contacts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(contacts, key = { "contact_${it.id}" }) { contact ->
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = 2.5.dp,
                    color = if (contact.isPrimaryCaregiver) EmeraldSuccess else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("emergency_contact_card_${contact.id}")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (contact.isPrimaryCaregiver) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "★ PRIMARY CAREGIVER",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${contact.relationship} • ${contact.phoneNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    onSpeakText("${contact.name}, ${contact.relationship}. Phone number: ${contact.phoneNumber}. ${contact.notes}")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Read contact aloud"
                                )
                            }
                            IconButton(onClick = { onDeleteContact(contact) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove contact",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (contact.notes.isNotBlank()) {
                        Text(
                            text = contact.notes,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            onSpeakText("Dialing ${contact.name} at ${contact.phoneNumber}")
                            dialPhoneNumber(contact.phoneNumber)
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 60.dp)
                            .testTag("dial_contact_${contact.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Call ${contact.name} (${contact.phoneNumber})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. Add Emergency Contact Form
        item {
            OutlinedButton(
                onClick = { showAddContact = !showAddContact },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (showAddContact) "Close Contact Form" else "Add Trusted Caregiver / Contact",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (showAddContact) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = contactName,
                            onValueChange = { contactName = it },
                            label = { Text("Name (e.g., Neighbor Robert)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = contactRel,
                            onValueChange = { contactRel = it },
                            label = { Text("Relationship") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = contactPhone,
                            onValueChange = { contactPhone = it },
                            label = { Text("Phone Number (e.g., 555-0144)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = contactNotes,
                            onValueChange = { contactNotes = it },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (contactName.isNotBlank() && contactPhone.isNotBlank()) {
                                    onAddContact(contactName, contactRel, contactPhone, contactNotes)
                                    contactName = ""
                                    contactPhone = ""
                                    contactNotes = ""
                                    showAddContact = false
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                        ) {
                            Text("Save Emergency Contact", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
