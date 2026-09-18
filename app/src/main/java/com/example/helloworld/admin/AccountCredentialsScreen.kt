package com.example.helloworld.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AccountCredentialsScreen(
    user: AdminUser,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Account Credentials", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Authenticated administrator identity",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CredentialRow(Icons.Default.AccountCircle, "Username", user.username)
                HorizontalDivider()
                CredentialRow(
                    Icons.Default.Email,
                    "Email",
                    user.email.ifBlank { "Email not available from the administrator session" }
                )
                HorizontalDivider()
                CredentialRow(
                    Icons.Default.VerifiedUser,
                    "Role",
                    if (user.role == "super_admin") "Super Admin" else user.role
                )
                HorizontalDivider()
                CredentialRow(
                    Icons.Default.Security,
                    "Account status",
                    if (user.is_active) "Active" else "Disabled"
                )
            }
        }

        Text(
            "Your administrator email is managed by the authenticated account. This screen is read-only.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CredentialRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Column(Modifier.padding(start = 14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
