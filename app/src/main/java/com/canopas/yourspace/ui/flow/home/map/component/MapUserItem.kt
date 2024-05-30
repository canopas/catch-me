package com.canopas.yourspace.ui.flow.home.map.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.UserProfile
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun MapUserItem(
    userInfo: UserInfo,
    onClick: () -> Unit
) {
    val user = userInfo.user
    Column(
        modifier = Modifier
            .width(40.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UserProfile(Modifier.size(40.dp), user = user)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = user.first_name ?: "",
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = AppTheme.appTypography.caption.copy(color = AppTheme.colorScheme.textPrimary)
        )
    }
}

@Composable
fun AddMemberBtn(
    showLoader: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(
                    1.dp,
                    AppTheme.colorScheme.primary.copy(alpha = 0.7f),
                    shape = CircleShape
                )
                .background(AppTheme.colorScheme.surface, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (showLoader) {
                AppProgressIndicator(strokeWidth = 2.dp)
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add_member),
                    modifier = Modifier.padding(10.dp),
                    contentDescription = "",
                    tint = AppTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(id = R.string.common_btn_add),
            style = AppTheme.appTypography.caption.copy(color = AppTheme.colorScheme.textPrimary)
        )
    }
}
