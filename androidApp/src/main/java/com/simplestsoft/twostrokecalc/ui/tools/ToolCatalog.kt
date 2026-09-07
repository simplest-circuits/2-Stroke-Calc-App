package com.simplestsoft.twostrokecalc.ui.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.ui.graphics.vector.ImageVector
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.ToolId
import com.simplestsoft.twostrokecalc.domain.toolDisplayOrder as sharedToolDisplayOrder

val toolDisplayOrder: List<ToolId> = sharedToolDisplayOrder

fun ToolId.tabLabelRes(): Int = when (this) {
    ToolId.COMMUNITY_SETUPS -> R.string.tool_tab_community
    ToolId.RPM_TACHOMETER -> R.string.tool_tab_rpm
    ToolId.PORT_TIMING_ASSIST -> R.string.tool_tab_port_timing
    ToolId.ANGLE_METER -> R.string.tool_tab_angle
    ToolId.VIBRATION_ANALYZER -> R.string.tool_tab_vibration
    ToolId.GPS_DYNO -> R.string.tool_tab_gps_dyno
}

fun ToolId.overviewDescriptionRes(): Int = when (this) {
    ToolId.COMMUNITY_SETUPS -> R.string.tool_overview_community_desc
    ToolId.RPM_TACHOMETER -> R.string.tool_overview_rpm_desc
    ToolId.PORT_TIMING_ASSIST -> R.string.tool_overview_port_timing_desc
    ToolId.ANGLE_METER -> R.string.tool_overview_angle_desc
    ToolId.VIBRATION_ANALYZER -> R.string.tool_overview_vibration_desc
    ToolId.GPS_DYNO -> R.string.tool_overview_gps_dyno_desc
}

fun ToolId.icon(): ImageVector = when (this) {
    ToolId.COMMUNITY_SETUPS -> Icons.Default.Groups
    ToolId.RPM_TACHOMETER -> Icons.Default.Speed
    ToolId.PORT_TIMING_ASSIST -> Icons.Default.Timeline
    ToolId.ANGLE_METER -> Icons.Default.Straighten
    ToolId.VIBRATION_ANALYZER -> Icons.Default.GraphicEq
    ToolId.GPS_DYNO -> Icons.Default.Build
}
