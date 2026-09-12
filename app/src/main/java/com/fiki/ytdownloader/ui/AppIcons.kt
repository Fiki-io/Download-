package com.fiki.ytdownloader.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object AppIcons {
    private fun vector(name: String, pathString: String): ImageVector {
        return ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).addPath(
            pathData = PathParser().parsePathString(pathString).toNodes(),
            fill = SolidColor(Color.Black)
        ).build()
    }

    val Download: ImageVector by lazy {
        vector("Download", "M5,20h14v-2H5V20z M19,9h-4V3H9v6H5l7,7L19,9z")
    }

    val Folder: ImageVector by lazy {
        vector("Folder", "M10,4H4c-1.1,0-1.99,0.9-1.99,2L2,18c0,1.1,0.9,2,2,2h16c1.1,0,2-0.9,2-2V8c0-1.1-0.9-2-2-2h-8l-2-2z")
    }

    val Headphones: ImageVector by lazy {
        vector("Headphones", "M12,3c-4.97,0-9,4.03-9,9v7c0,1.1,0.9,2,2,2h4v-8H5v-1c0-3.87,3.13-7,7-7s7,3.13,7,7v1h-4v8h4c1.1,0,2-0.9,2-2v-7c0-4.97-4.03-9-9-9z")
    }

    val Movie: ImageVector by lazy {
        vector("Movie", "M18,4l2,4h-3l-2-4h-2l2,4h-3l-2-4H8l2,4H7L5,4H4c-1.1,0-1.99,0.9-1.99,2L2,18c0,1.1,0.9,2,2,2h16c1.1,0,2-0.9,2-2V4H18z")
    }

    val ContentPaste: ImageVector by lazy {
        vector("ContentPaste", "M19,2h-4.18C14.4,0.84,13.3,0,12,0c-1.3,0-2.4,0.84-2.82,2H5c-1.1,0-2,0.9-2,2v16c0,1.1,0.9,2,2,2h14c1.1,0,2-0.9,2-2V4c0-1.1-0.9-2-2-2zm-7,0c0.55,0,1,0.45,1,1s-0.45,1-1,1-1-0.45-1-1,0.45-1,1-1zm7,18H5V4h2v3h10V4h2v16z")
    }

    val Bolt: ImageVector by lazy {
        vector("Bolt", "M11 21h-1l1-7H7.5c-.58 0-.57-.32-.38-.66.19-.34.05-.08.07-.12C8.48 10.94 10.42 7.54 13 3h1l-1 7h3.5c.49 0 .56.33.47.51l-.07.15C12.9 17.55 11 21 11 21z")
    }
}
