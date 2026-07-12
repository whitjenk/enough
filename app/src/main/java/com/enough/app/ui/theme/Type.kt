package com.enough.app.ui.theme

import androidx.compose.material3.Typography

/**
 * Phase 0 uses the default M3 type scale (DESIGN.md explicitly allows this and
 * says not to block shipping on Roboto Flex integration). Live-updating numbers
 * get their weight/size emphasis at the call site, not by re-theming globally,
 * so color never has to carry that signal.
 */
val EnoughTypography = Typography()
