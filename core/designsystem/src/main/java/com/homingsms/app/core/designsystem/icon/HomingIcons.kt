@file:Suppress("ktlint:standard:max-line-length", "MaxLineLength", "ktlint:standard:argument-list-wrapping")

package com.homingsms.app.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

object HomingIcons {
    private val Ink = SolidColor(Color.Black)

    private inline fun icon(name: String, autoMirror: Boolean = false, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = autoMirror
        )
            .apply(block)
            .build()

    val Home: ImageVector by lazy {
        icon("Home") {
            addPath(pathData = addPathNodes("M13 13.5032H11C10.1716 13.5032 9.5 14.1748 9.5 15.0032V20.5012C9.5 21.0539 9.05114 21.5032 8.49854 21.4977C6.17337 21.4744 4.88133 21.3333 4.02513 20.4771C3 19.452 3 17.8021 3 14.5023V11.9918C3 10.3106 3 9.46999 3.3559 8.74231C3.71179 8.01463 4.37533 7.49854 5.70241 6.46637L7.70242 4.91081C9.76689 3.30511 10.7991 2.50226 12 2.50226C13.2009 2.50226 14.2331 3.30511 16.2976 4.91081L18.2976 6.46636C19.6247 7.49854 20.2882 8.01463 20.6441 8.74231C21 9.46999 21 10.3106 21 11.9918V14.5023C21 17.8021 21 19.452 19.9749 20.4771C19.1187 21.3333 17.8266 21.4744 15.5015 21.4977C14.9489 21.5032 14.5 21.0539 14.5 20.5012V15.0032C14.5 14.1748 13.8284 13.5032 13 13.5032Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Messages: ImageVector by lazy {
        icon("Messages") {
            addPath(pathData = addPathNodes("M8.5 14.5H15.5M8.5 9.5H12"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M14.1706 20.8905C18.3536 20.6125 21.6856 17.2332 21.9598 12.9909C22.0134 12.1607 22.0134 11.3009 21.9598 10.4707C21.6856 6.22838 18.3536 2.84913 14.1706 2.57107C12.7435 2.47621 11.2536 2.47641 9.8294 2.57107C5.64639 2.84913 2.31441 6.22838 2.04024 10.4707C1.98659 11.3009 1.98659 12.1607 2.04024 12.9909C2.1401 14.536 2.82343 15.9666 3.62791 17.1746C4.09501 18.0203 3.78674 19.0758 3.30021 19.9978C2.94941 20.6626 2.77401 20.995 2.91484 21.2351C3.05568 21.4752 3.37026 21.4829 3.99943 21.4982C5.24367 21.5285 6.08268 21.1757 6.74868 20.6846C7.1264 20.4061 7.31527 20.2668 7.44544 20.2508C7.5756 20.2348 7.83177 20.3403 8.34401 20.5513C8.8044 20.7409 9.33896 20.8579 9.8294 20.8905C11.2536 20.9852 12.7435 20.9854 14.1706 20.8905Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Inbox: ImageVector by lazy {
        icon("Inbox") {
            addPath(pathData = addPathNodes("M2.5 12C2.5 7.52166 2.5 5.28249 3.89124 3.89124C5.28249 2.5 7.52166 2.5 12 2.5C16.4783 2.5 18.7175 2.5 20.1088 3.89124C21.5 5.28249 21.5 7.52166 21.5 12C21.5 16.4783 21.5 18.7175 20.1088 20.1088C18.7175 21.5 16.4783 21.5 12 21.5C7.52166 21.5 5.28249 21.5 3.89124 20.1088C2.5 18.7175 2.5 16.4783 2.5 12Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M21.5 13.5H16.5743C15.7322 13.5 15.0706 14.2036 14.6995 14.9472C14.2963 15.7551 13.4889 16.5 12 16.5C10.5111 16.5 9.70373 15.7551 9.30054 14.9472C8.92942 14.2036 8.26777 13.5 7.42566 13.5H2.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Connect: ImageVector by lazy {
        icon("Connect") {
            addPath(pathData = addPathNodes("M4.51255 19.4866C7.02498 21.8794 10.016 20.9223 11.2124 19.9532C11.8314 19.4518 12.1097 19.1277 12.3489 18.8884C13.1864 18.1107 13.1326 17.3331 12.5882 16.711C12.3704 16.462 10.9731 15.1198 9.63313 13.7439C8.93922 13.0499 8.46066 12.5595 8.05149 12.1647C7.50354 11.6185 7.02499 10.9922 6.30715 11.0101C5.64913 11.0101 5.17057 11.5904 4.57237 12.1886C3.88422 12.8767 3.37598 13.7439 3.19652 14.5216C2.65814 16.7947 3.49562 18.4098 4.51255 19.4866ZM4.51255 19.4866L2.00012 21.999"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M19.4867 4.51472C16.9736 2.12078 13.9929 3.09593 12.7962 4.06548C12.177 4.56712 11.8987 4.89138 11.6593 5.13078C10.8216 5.90881 10.8755 6.68683 11.42 7.30926C11.4983 7.39881 11.7292 7.62975 12.055 7.95281M19.4867 4.51472C20.504 5.59199 21.3528 7.22547 20.8142 9.49971C20.6347 10.2777 20.1264 11.1453 19.438 11.8338C18.8397 12.4323 18.361 13.0128 17.7028 13.0128C16.9847 13.0308 16.6121 12.5115 16.064 11.9651M19.4867 4.51472L21.9999 2.0011M16.064 11.9651C15.6547 11.5701 15.07 10.9721 14.3759 10.2777C13.5175 9.39612 12.6355 8.52831 12.055 7.95281M16.064 11.9651L14.5024 13.4896M10.5114 9.50609L12.055 7.95281"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
        }
    }

    val Settings: ImageVector by lazy {
        icon("Settings") {
            addPath(pathData = addPathNodes("M21.3175 7.14139L20.8239 6.28479C20.4506 5.63696 20.264 5.31305 19.9464 5.18388C19.6288 5.05472 19.2696 5.15664 18.5513 5.36048L17.3311 5.70418C16.8725 5.80994 16.3913 5.74994 15.9726 5.53479L15.6357 5.34042C15.2766 5.11043 15.0004 4.77133 14.8475 4.37274L14.5136 3.37536C14.294 2.71534 14.1842 2.38533 13.9228 2.19657C13.6615 2.00781 13.3143 2.00781 12.6199 2.00781H11.5051C10.8108 2.00781 10.4636 2.00781 10.2022 2.19657C9.94085 2.38533 9.83106 2.71534 9.61149 3.37536L9.27753 4.37274C9.12465 4.77133 8.84845 5.11043 8.48937 5.34042L8.15249 5.53479C7.73374 5.74994 7.25259 5.80994 6.79398 5.70418L5.57375 5.36048C4.85541 5.15664 4.49625 5.05472 4.17867 5.18388C3.86109 5.31305 3.67445 5.63696 3.30115 6.28479L2.80757 7.14139C2.45766 7.74864 2.2827 8.05227 2.31666 8.37549C2.35061 8.69871 2.58483 8.95918 3.05326 9.48012L4.0843 10.6328C4.3363 10.9518 4.51521 11.5078 4.51521 12.0077C4.51521 12.5078 4.33636 13.0636 4.08433 13.3827L3.05326 14.5354C2.58483 15.0564 2.35062 15.3168 2.31666 15.6401C2.2827 15.9633 2.45766 16.2669 2.80757 16.8741L3.30114 17.7307C3.67443 18.3785 3.86109 18.7025 4.17867 18.8316C4.49625 18.9608 4.85542 18.8589 5.57377 18.655L6.79394 18.3113C7.25263 18.2055 7.73387 18.2656 8.15267 18.4808L8.4895 18.6752C8.84851 18.9052 9.12464 19.2442 9.2775 19.6428L9.61149 20.6403C9.83106 21.3003 9.94085 21.6303 10.2022 21.8191C10.4636 22.0078 10.8108 22.0078 11.5051 22.0078H12.6199C13.3143 22.0078 13.6615 22.0078 13.9228 21.8191C14.1842 21.6303 14.294 21.3003 14.5136 20.6403L14.8476 19.6428C15.0004 19.2442 15.2765 18.9052 15.6356 18.6752L15.9724 18.4808C16.3912 18.2656 16.8724 18.2055 17.3311 18.3113L18.5513 18.655C19.2696 18.8589 19.6288 18.9608 19.9464 18.8316C20.264 18.7025 20.4506 18.3785 20.8239 17.7307L21.3175 16.8741C21.6674 16.2669 21.8423 15.9633 21.8084 15.6401C21.7744 15.3168 21.5402 15.0564 21.0718 14.5354L20.0407 13.3827C19.7887 13.0636 19.6098 12.5078 19.6098 12.0077C19.6098 11.5078 19.7888 10.9518 20.0407 10.6328L21.0718 9.48012C21.5402 8.95918 21.7744 8.69871 21.8084 8.37549C21.8423 8.05227 21.6674 7.74864 21.3175 7.14139Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M15.5195 12C15.5195 13.933 13.9525 15.5 12.0195 15.5C10.0865 15.5 8.51953 13.933 8.51953 12C8.51953 10.067 10.0865 8.5 12.0195 8.5C13.9525 8.5 15.5195 10.067 15.5195 12Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
        }
    }

    val Back: ImageVector by lazy {
        icon("Back", autoMirror = true) {
            addPath(pathData = addPathNodes("M15 6C15 6 9.00001 10.4189 9 12C8.99999 13.5812 15 18 15 18"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight", autoMirror = true) {
            addPath(pathData = addPathNodes("M9.00005 6C9.00005 6 15 10.4189 15 12C15 13.5812 9 18 9 18"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val ChevronDown: ImageVector by lazy {
        icon("ChevronDown") {
            addPath(pathData = addPathNodes("M18 9.00005C18 9.00005 13.5811 15 12 15C10.4188 15 6 9 6 9"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Close: ImageVector by lazy {
        icon("Close") {
            addPath(pathData = addPathNodes("M18 6L6.00081 17.9992M17.9992 18L6 6.00085"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Check: ImageVector by lazy {
        icon("Check") {
            addPath(pathData = addPathNodes("M5 14L8.5 17.5L19 6.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val CheckboxChecked: ImageVector by lazy {
        icon("CheckboxChecked") {
            addPath(pathData = addPathNodes("M2.5 12C2.5 7.52166 2.5 5.28249 3.89124 3.89124C5.28249 2.5 7.52166 2.5 12 2.5C16.4783 2.5 18.7175 2.5 20.1088 3.89124C21.5 5.28249 21.5 7.52166 21.5 12C21.5 16.4783 21.5 18.7175 20.1088 20.1088C18.7175 21.5 16.4783 21.5 12 21.5C7.52166 21.5 5.28249 21.5 3.89124 20.1088C2.5 18.7175 2.5 16.4783 2.5 12Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M8 12.5L10.5 15L16 9"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val CheckboxEmpty: ImageVector by lazy {
        icon("CheckboxEmpty") {
            addPath(pathData = addPathNodes("M2.5 12C2.5 7.52166 2.5 5.28249 3.89124 3.89124C5.28249 2.5 7.52166 2.5 12 2.5C16.4783 2.5 18.7175 2.5 20.1088 3.89124C21.5 5.28249 21.5 7.52166 21.5 12C21.5 16.4783 21.5 18.7175 20.1088 20.1088C18.7175 21.5 16.4783 21.5 12 21.5C7.52166 21.5 5.28249 21.5 3.89124 20.1088C2.5 18.7175 2.5 16.4783 2.5 12Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
        }
    }

    val CheckCircle: ImageVector by lazy {
        icon("CheckCircle") {
            addPath(pathData = addPathNodes("M22 12C22 6.47715 17.5228 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22C17.5228 22 22 17.5228 22 12Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M8 12.5L10.5 15L16 9"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Add: ImageVector by lazy {
        icon("Add") {
            addPath(pathData = addPathNodes("M12.001 5.00003V19.002"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M19.002 12.002L4.99998 12.002"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Edit: ImageVector by lazy {
        icon("Edit") {
            addPath(pathData = addPathNodes("M16.4249 4.60509L17.4149 3.6151C18.2351 2.79497 19.5648 2.79497 20.3849 3.6151C21.205 4.43524 21.205 5.76493 20.3849 6.58507L19.3949 7.57506M16.4249 4.60509L9.76558 11.2644C9.25807 11.772 8.89804 12.4078 8.72397 13.1041L8 16L10.8959 15.276C11.5922 15.102 12.228 14.7419 12.7356 14.2344L19.3949 7.57506M16.4249 4.60509L19.3949 7.57506"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M18.9999 13.5C18.9999 16.7875 18.9999 18.4312 18.092 19.5376C17.9258 19.7401 17.7401 19.9258 17.5375 20.092C16.4312 21 14.7874 21 11.4999 21H11C7.22876 21 5.34316 21 4.17159 19.8284C3.00003 18.6569 3 16.7712 3 13V12.5C3 9.21252 3 7.56879 3.90794 6.46244C4.07417 6.2599 4.2599 6.07417 4.46244 5.90794C5.56879 5 7.21252 5 10.5 5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Delete: ImageVector by lazy {
        icon("Delete") {
            addPath(pathData = addPathNodes("M19.5 5.5L18.8803 15.5251C18.7219 18.0864 18.6428 19.3671 18.0008 20.2879C17.6833 20.7431 17.2747 21.1273 16.8007 21.416C15.8421 22 14.559 22 11.9927 22C9.42312 22 8.1383 22 7.17905 21.4149C6.7048 21.1257 6.296 20.7408 5.97868 20.2848C5.33688 19.3626 5.25945 18.0801 5.10461 15.5152L4.5 5.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M3 5.5H21M16.0557 5.5L15.3731 4.09173C14.9196 3.15626 14.6928 2.68852 14.3017 2.39681C14.215 2.3321 14.1231 2.27454 14.027 2.2247C13.5939 2 13.0741 2 12.0345 2C10.9688 2 10.436 2 9.99568 2.23412C9.8981 2.28601 9.80498 2.3459 9.71729 2.41317C9.32164 2.7167 9.10063 3.20155 8.65861 4.17126L8.05292 5.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M9.5 16.5L9.5 10.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M14.5 16.5L14.5 10.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
        }
    }

    val Copy: ImageVector by lazy {
        icon("Copy") {
            addPath(pathData = addPathNodes("M7.5 14.5C7.5 11.2002 7.5 9.55025 8.52513 8.52513C9.55025 7.5 11.2002 7.5 14.5 7.5C17.7998 7.5 19.4497 7.5 20.4749 8.52513C21.5 9.55025 21.5 11.2002 21.5 14.5C21.5 17.7998 21.5 19.4497 20.4749 20.4749C19.4497 21.5 17.7998 21.5 14.5 21.5C11.2002 21.5 9.55025 21.5 8.52513 20.4749C7.5 19.4497 7.5 17.7998 7.5 14.5Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M7.5 16.5C6.10355 16.5 5.40533 16.5 4.84402 16.3036C3.83866 15.9518 3.0482 15.1613 2.69641 14.156C2.5 13.5947 2.5 12.8964 2.5 11.5V9.5C2.5 6.20017 2.5 4.55025 3.52513 3.52513C4.55025 2.5 6.20017 2.5 9.5 2.5H11.5C12.8964 2.5 13.5947 2.5 14.156 2.69641C15.1613 3.0482 15.9518 3.83866 16.3036 4.84402C16.5 5.40533 16.5 6.10355 16.5 7.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Share: ImageVector by lazy {
        icon("Share") {
            addPath(pathData = addPathNodes("M21 6.5C21 8.15685 19.6569 9.5 18 9.5C16.3431 9.5 15 8.15685 15 6.5C15 4.84315 16.3431 3.5 18 3.5C19.6569 3.5 21 4.84315 21 6.5Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M9 12C9 13.6569 7.65685 15 6 15C4.34315 15 3 13.6569 3 12C3 10.3431 4.34315 9 6 9C7.65685 9 9 10.3431 9 12Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M21 17.5C21 19.1569 19.6569 20.5 18 20.5C16.3431 20.5 15 19.1569 15 17.5C15 15.8431 16.3431 14.5 18 14.5C19.6569 14.5 21 15.8431 21 17.5Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M8.72852 10.7495L15.2285 7.75M8.72852 13.25L15.2285 16.2495"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
        }
    }

    val Search: ImageVector by lazy {
        icon("Search") {
            addPath(pathData = addPathNodes("M17 17L21 21"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M19 11C19 6.58172 15.4183 3 11 3C6.58172 3 3 6.58172 3 11C3 15.4183 6.58172 19 11 19C15.4183 19 19 15.4183 19 11Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val FilterOff: ImageVector by lazy {
        icon("FilterOff") {
            addPath(pathData = addPathNodes("M20.9987 4.5C20.9869 4.06504 20.8956 3.75346 20.672 3.5074C20.2111 3 19.396 3 17.7657 3H6.23433C4.60404 3 3.7889 3 3.32795 3.5074C2.86701 4.0148 2.96811 4.8008 3.17033 6.3728C3.22938 6.8319 3.3276 7.09253 3.62734 7.44867C4.59564 8.59915 6.36901 10.6456 8.85746 12.5061C9.08486 12.6761 9.23409 12.9539 9.25927 13.2614C9.53961 16.6864 9.79643 19.0261 9.93278 20.1778C10.0043 20.782 10.6741 21.2466 11.226 20.8563C12.1532 20.2006 13.8853 19.4657 14.1141 18.2442C14.1986 17.7934 14.3136 17.0803 14.445 16"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M21 7L15 13M21 13L15 7"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Refresh: ImageVector by lazy {
        icon("Refresh") {
            addPath(pathData = addPathNodes("M20.5 5.5H9.5C5.78672 5.5 3 8.18503 3 12"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M3.5 18.5H14.5C18.2133 18.5 21 15.815 21 12"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M18.5 3C18.5 3 21 4.84122 21 5.50002C21 6.15882 18.5 8 18.5 8"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M5.49998 16C5.49998 16 3.00001 17.8412 3 18.5C2.99999 19.1588 5.5 21 5.5 21"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Sync: ImageVector by lazy {
        icon("Sync") {
            addPath(pathData = addPathNodes("M15 19L15 6.65856C15 5.65277 15 5.14987 15.3087 5.02472C15.6173 4.89956 15.9806 5.25517 16.7071 5.96637L19 8.21091"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M9 5L9 17.3414C9 18.3472 9 18.8501 8.69134 18.9753C8.38268 19.1004 8.01942 18.7448 7.29289 18.0336L5 15.7891"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Transfer: ImageVector by lazy {
        icon("Transfer") {
            addPath(pathData = addPathNodes("M19 9H6.65856C5.65277 9 5.14987 9 5.02472 8.69134C4.89957 8.38268 5.25517 8.01942 5.96637 7.29289L8.21091 5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M5 15H17.3414C18.3472 15 18.8501 15 18.9753 15.3087C19.1004 15.6173 18.7448 15.9806 18.0336 16.7071L15.7891 19"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Send: ImageVector by lazy {
        icon("Send", autoMirror = true) {
            addPath(pathData = addPathNodes("M21.0477 3.05293C18.8697 0.707363 2.48648 6.4532 2.50001 8.551C2.51535 10.9299 8.89809 11.6617 10.6672 12.1581C11.7311 12.4565 12.016 12.7625 12.2613 13.8781C13.3723 18.9305 13.9301 21.4435 15.2014 21.4996C17.2278 21.5892 23.1733 5.342 21.0477 3.05293Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M11.4999 12.5L14.9999 9"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Server: ImageVector by lazy {
        icon("Server") {
            addPath(pathData = addPathNodes("M19 4H5C4.06812 4 3.60218 4 3.23463 4.15224C2.74458 4.35523 2.35523 4.74458 2.15224 5.23463C2 5.60218 2 6.06812 2 7C2 7.93188 2 8.39782 2.15224 8.76537C2.35523 9.25542 2.74458 9.64477 3.23463 9.84776C3.60218 10 4.06812 10 5 10H19C19.9319 10 20.3978 10 20.7654 9.84776C21.2554 9.64477 21.6448 9.25542 21.8478 8.76537C22 8.39782 22 7.93188 22 7C22 6.06812 22 5.60218 21.8478 5.23463C21.6448 4.74458 21.2554 4.35523 20.7654 4.15224C20.3978 4 19.9319 4 19 4Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M19 14H5C4.06812 14 3.60218 14 3.23463 14.1522C2.74458 14.3552 2.35523 14.7446 2.15224 15.2346C2 15.6022 2 16.0681 2 17C2 17.9319 2 18.3978 2.15224 18.7654C2.35523 19.2554 2.74458 19.6448 3.23463 19.8478C3.60218 20 4.06812 20 5 20H19C19.9319 20 20.3978 20 20.7654 19.8478C21.2554 19.6448 21.6448 19.2554 21.8478 18.7654C22 18.3978 22 17.9319 22 17C22 16.0681 22 15.6022 21.8478 15.2346C21.6448 14.7446 21.2554 14.3552 20.7654 14.1522C20.3978 14 19.9319 14 19 14Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M6.125 7H6M6.25 7C6.25 7.13807 6.13807 7.25 6 7.25C5.86193 7.25 5.75 7.13807 5.75 7C5.75 6.86193 5.86193 6.75 6 6.75C6.13807 6.75 6.25 6.86193 6.25 7Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M10.125 7H10M10.25 7C10.25 7.13807 10.1381 7.25 10 7.25C9.86193 7.25 9.75 7.13807 9.75 7C9.75 6.86193 9.86193 6.75 10 6.75C10.1381 6.75 10.25 6.86193 10.25 7Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M6.125 17H6M6.25 17C6.25 17.1381 6.13807 17.25 6 17.25C5.86193 17.25 5.75 17.1381 5.75 17C5.75 16.8619 5.86193 16.75 6 16.75C6.13807 16.75 6.25 16.8619 6.25 17Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M10.125 17H10M10.25 17C10.25 17.1381 10.1381 17.25 10 17.25C9.86193 17.25 9.75 17.1381 9.75 17C9.75 16.8619 9.86193 16.75 10 16.75C10.1381 16.75 10.25 16.8619 10.25 17Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Cloud: ImageVector by lazy {
        icon("Cloud") {
            addPath(pathData = addPathNodes("M17.4776 10.0001C17.485 10 17.4925 10 17.5 10C19.9853 10 22 12.0147 22 14.5C22 16.9853 19.9853 19 17.5 19H7C4.23858 19 2 16.7614 2 14C2 11.4003 3.98398 9.26407 6.52042 9.0227M17.4776 10.0001C17.4924 9.83536 17.5 9.66856 17.5 9.5C17.5 6.46243 15.0376 4 12 4C9.12324 4 6.76233 6.20862 6.52042 9.0227M17.4776 10.0001C17.3753 11.1345 16.9286 12.1696 16.2428 13M6.52042 9.0227C6.67826 9.00768 6.83823 9 7 9C8.12582 9 9.16474 9.37209 10.0005 10"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val CloudOff: ImageVector by lazy {
        icon("CloudOff") {
            addPath(pathData = addPathNodes("M17.4776 10.5001C17.485 10.5 17.4925 10.5 17.5 10.5C19.9853 10.5 22 12.5147 22 15C22 15.8582 21.7597 16.6604 21.3428 17.3428M17.4776 10.5001C17.4924 10.3354 17.5 10.1686 17.5 10C17.5 6.96243 15.0376 4.5 12 4.5C10.9945 4.5 10.052 4.76982 9.24101 5.24101M17.4776 10.5001C17.4039 11.3178 17.1512 12.0839 16.759 12.759M6.52042 9.5227C3.98398 9.76407 2 11.9003 2 14.5C2 17.2614 4.23858 19.5 7 19.5H17.5C18.0928 19.5 18.6588 19.3854 19.1771 19.1771M6.52042 9.5227C6.67826 9.50768 6.83823 9.5 7 9.5C8.12582 9.5 9.16474 9.87209 10.0005 10.5M6.52042 9.5227C6.59145 8.69641 6.84518 7.92232 7.24101 7.24101"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M2 2L22 22"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Webhook: ImageVector by lazy {
        icon("Webhook") {
            addPath(pathData = addPathNodes("M5.062 13C3.83229 13.6824 3 14.994 3 16.5C3 18.7091 4.79086 20.5 7 20.5C9.20914 20.5 11 18.7091 11 16.5H17"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M12 7.5L15.0571 13.0027C15.6323 12.6825 16.2949 12.5 17 12.5C19.2091 12.5 21 14.2909 21 16.5C21 18.7091 19.2091 20.5 17 20.5C16.0541 20.5 15.1848 20.1716 14.5 19.6227"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M12 8.5C12.5523 8.5 13 8.05228 13 7.5C13 6.94772 12.5523 6.5 12 6.5M12 8.5C11.4477 8.5 11 8.05228 11 7.5C11 6.94772 11.4477 6.5 12 6.5M12 8.5V6.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M7 17.5C7.55228 17.5 8 17.0523 8 16.5C8 15.9477 7.55228 15.5 7 15.5M7 17.5C6.44772 17.5 6 17.0523 6 16.5C6 15.9477 6.44772 15.5 7 15.5M7 17.5V15.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M17 17.5C17.5523 17.5 18 17.0523 18 16.5C18 15.9477 17.5523 15.5 17 15.5M17 17.5C16.4477 17.5 16 17.0523 16 16.5C16 15.9477 16.4477 15.5 17 15.5M17 17.5V15.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M16 7.5C16 5.29086 14.2091 3.5 12 3.5C9.79086 3.5 8 5.29086 8 7.5C8 9.004 8.83007 10.3141 10.0571 10.9973L7 16.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val QrScan: ImageVector by lazy {
        icon("QrScan") {
            addPath(pathData = addPathNodes("M2.5 8.18677C2.60406 6.08705 2.91537 4.77792 3.84664 3.84664C4.77792 2.91537 6.08705 2.60406 8.18677 2.5M21.5 8.18677C21.3959 6.08705 21.0846 4.77792 20.1534 3.84664C19.2221 2.91537 17.9129 2.60406 15.8132 2.5M15.8132 21.5C17.9129 21.3959 19.2221 21.0846 20.1534 20.1534C21.0846 19.2221 21.3959 17.9129 21.5 15.8132M8.18676 21.5C6.08705 21.3959 4.77792 21.0846 3.84664 20.1534C2.91537 19.2221 2.60406 17.9129 2.5 15.8132"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M8.23463 12.8478C8.60218 13 9.06812 13 10 13C10.9319 13 11.3978 13 11.7654 12.8478C12.2554 12.6448 12.6448 12.2554 12.8478 11.7654C13 11.3978 13 10.9319 13 10C13 9.06812 13 8.60218 12.8478 8.23463C12.6448 7.74458 12.2554 7.35523 11.7654 7.15224C11.3978 7 10.9319 7 10 7C9.06812 7 8.60218 7 8.23463 7.15224C7.74458 7.35523 7.35523 7.74458 7.15224 8.23463C7 8.60218 7 9.06812 7 10C7 10.9319 7 11.3978 7.15224 11.7654C7.35523 12.2554 7.74458 12.6448 8.23463 12.8478Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M17 7V7.01"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M17 11V13C17 14.8856 17 15.8284 16.4142 16.4142C15.8284 17 14.8856 17 13 17"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M9 17H7"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Camera: ImageVector by lazy {
        icon("Camera") {
            addPath(pathData = addPathNodes("M12.6974 3.5H11.303C10.5884 3.5 10.2311 3.5 9.91067 3.612C9.71499 3.68039 9.53113 3.77879 9.36568 3.90367C9.09474 4.10816 8.89655 4.40544 8.50018 5L8.50017 5.00001C8.29717 5.30453 7.99794 5.75337 7.87867 5.87871C7.58314 6.18927 7.19563 6.39666 6.77329 6.47029C6.60284 6.5 6.41985 6.5 6.05387 6.5C5.07379 6.5 4.58376 6.5 4.18307 6.61342C3.18074 6.89716 2.39734 7.68055 2.1136 8.68289C2.00018 9.08357 2.00018 9.57361 2.00018 10.5537V14.5C2.00018 17.3284 2.00018 18.7426 2.87886 19.6213C3.75754 20.5 5.17176 20.5 8.00018 20.5H16.0002C18.8286 20.5 20.2428 20.5 21.1215 19.6213C22.0002 18.7426 22.0002 17.3284 22.0002 14.5V10.5537C22.0002 9.57361 22.0002 9.08357 21.8868 8.68289C21.603 7.68055 20.8196 6.89716 19.8173 6.61342C19.4166 6.5 18.9266 6.5 17.9465 6.5C17.5805 6.5 17.3975 6.5 17.2271 6.47029C16.8047 6.39666 16.4172 6.18927 16.1217 5.87871C16.0024 5.75336 15.7032 5.30451 15.5002 5C15.1038 4.40544 14.9056 4.10816 14.6347 3.90367C14.4692 3.77879 14.2854 3.68039 14.0897 3.612C13.7693 3.5 13.412 3.5 12.6974 3.5Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M16.0002 13C16.0002 15.2091 14.2093 17 12.0002 17C9.79104 17 8.00018 15.2091 8.00018 13C8.00018 10.7909 9.79104 9 12.0002 9C14.2093 9 16.0002 10.7909 16.0002 13Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M19.1252 9.5H19.0002M19.2502 9.5C19.2502 9.63807 19.1383 9.75 19.0002 9.75C18.8621 9.75 18.7502 9.63807 18.7502 9.5C18.7502 9.36193 18.8621 9.25 19.0002 9.25C19.1383 9.25 19.2502 9.36193 19.2502 9.5Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
        }
    }

    val SimCard: ImageVector by lazy {
        icon("SimCard") {
            addPath(pathData = addPathNodes("M3 11.8584C3 7.28199 3 4.99376 4.38674 3.54394C4.43797 3.49038 4.49038 3.43797 4.54394 3.38674C5.99376 2 8.28199 2 12.8584 2C13.943 2 14.4655 2.00376 14.9628 2.18936C15.4417 2.3681 15.8429 2.70239 16.6452 3.37099L18.8411 5.20092C19.9027 6.08561 20.4335 6.52795 20.7168 7.13266C21 7.73737 21 8.42833 21 9.81025V13C21 16.7497 21 18.6246 20.0451 19.9389C19.7367 20.3634 19.3634 20.7367 18.9389 21.0451C17.6246 22 15.7497 22 12 22C8.25027 22 6.3754 22 5.06107 21.0451C4.6366 20.7367 4.26331 20.3634 3.95491 19.9389C3 18.6246 3 16.7497 3 13V11.8584Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M7 14.5C7 16.1499 7 16.9749 7.43934 17.4874C7.87868 18 8.58579 18 10 18H14C15.4142 18 16.1213 18 16.5607 17.4874C17 16.9749 17 16.1499 17 14.5M7 14.5C7 12.8501 7 12.0251 7.43934 11.5126C7.87868 11 8.58579 11 10 11H14C15.4142 11 16.1213 11 16.5607 11.5126C17 12.0251 17 12.8501 17 14.5M7 14.5H9M17 14.5H15"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Battery: ImageVector by lazy {
        icon("Battery") {
            addPath(pathData = addPathNodes("M2 12C2 9.17157 2 7.75736 2.87868 6.87868C3.75736 6 5.17157 6 8 6H13C15.8284 6 17.2426 6 18.1213 6.87868C19 7.75736 19 9.17157 19 12C19 14.8284 19 16.2426 18.1213 17.1213C17.2426 18 15.8284 18 13 18H8C5.17157 18 3.75736 18 2.87868 17.1213C2 16.2426 2 14.8284 2 12Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M19 9.5L20.0272 9.6712C20.7085 9.78475 21.0491 9.84152 21.3076 10.0067C21.5618 10.1691 21.7612 10.4044 21.8796 10.6819C22 10.964 22 11.3093 22 12C22 12.6907 22 13.036 21.8796 13.3181C21.7612 13.5956 21.5618 13.8309 21.3076 13.9933C21.0491 14.1585 20.7085 14.2153 20.0272 14.3288L19 14.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M6 10V14"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M9 10V14"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M12 10V14"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M15 10V14"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
        }
    }

    val BatteryCharging: ImageVector by lazy {
        icon("BatteryCharging") {
            addPath(pathData = addPathNodes("M2 12C2 9.17157 2 7.75736 2.87868 6.87868C3.75736 6 5.17157 6 8 6H13C15.8284 6 17.2426 6 18.1213 6.87868C19 7.75736 19 9.17157 19 12C19 14.8284 19 16.2426 18.1213 17.1213C17.2426 18 15.8284 18 13 18H8C5.17157 18 3.75736 18 2.87868 17.1213C2 16.2426 2 14.8284 2 12Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M10.8282 9L9.08572 11.1749C8.89899 11.4079 9.03283 11.7433 9.33733 11.8053L11.1627 12.1773C11.4873 12.2434 11.6111 12.6147 11.3842 12.8413L9.22216 15"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M19 9.5L20.0272 9.6712C20.7085 9.78475 21.0491 9.84152 21.3076 10.0067C21.5618 10.1691 21.7612 10.4044 21.8796 10.6819C22 10.964 22 11.3093 22 12C22 12.6907 22 13.036 21.8796 13.3181C21.7612 13.5956 21.5618 13.8309 21.3076 13.9933C21.0491 14.1585 20.7085 14.2153 20.0272 14.3288L19 14.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
        }
    }

    val Wifi: ImageVector by lazy {
        icon("Wifi") {
            addPath(pathData = addPathNodes("M8.25 14.5C10.25 12.5 13.75 12.5 15.75 14.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M18.5 11.5C14.7324 8.16667 9.5 8.16667 5.5 11.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M2 8.5C8.31579 3.16669 15.6842 3.16668 22 8.49989"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M10.5,18A1.5,1.5 0 1,0 13.5,18A1.5,1.5 0 1,0 10.5,18Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Cellular: ImageVector by lazy {
        icon("Cellular") {
            addPath(pathData = addPathNodes("M12 11L12 20"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M14 9C14 10.1046 13.1046 11 12 11C10.8954 11 10 10.1046 10 9C10 7.89543 10.8954 7 12 7C13.1046 7 14 7.89543 14 9Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M16.9588 6C17.6186 6.86961 18 7.89801 18 9C18 10.102 17.6186 11.1304 16.9588 12M7.04117 6C6.38143 6.86961 6 7.89801 6 9C6 10.102 6.38143 11.1304 7.04117 12"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M20.3159 4C21.3796 5.43008 22 7.14984 22 9C22 10.8502 21.3796 12.5699 20.3159 14M3.68409 4C2.62036 5.43008 2 7.14984 2 9C2 10.8502 2.62036 12.5699 3.68409 14"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Network: ImageVector by lazy {
        icon("Network") {
            addPath(pathData = addPathNodes("M2,12A10,10 0 1,0 22,12A10,10 0 1,0 2,12Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M8 12C8 18 12 22 12 22C12 22 16 18 16 12C16 6 12 2 12 2C12 2 8 6 8 12Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M21 15H3"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M21 9H3"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Attachment: ImageVector by lazy {
        icon("Attachment") {
            addPath(pathData = addPathNodes("M5.82338 12L4.27922 10.4558C2.57359 8.75022 2.57359 5.98485 4.27922 4.27922C5.98485 2.57359 8.75022 2.57359 10.4558 4.27922L19.7208 13.5442C21.4264 15.2498 21.4264 18.0152 19.7208 19.7208C18.0152 21.4264 15.2498 21.4264 13.5442 19.7208L10.0698 16.2464C9.00379 15.1804 9.00379 13.4521 10.0698 12.386C11.1358 11.32 12.8642 11.32 13.9302 12.386L15.8604 14.3162"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Image: ImageVector by lazy {
        icon("Image") {
            addPath(pathData = addPathNodes("M6,7.5A1.5,1.5 0 1,0 9,7.5A1.5,1.5 0 1,0 6,7.5Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M2.5 12C2.5 7.52166 2.5 5.28249 3.89124 3.89124C5.28249 2.5 7.52166 2.5 12 2.5C16.4783 2.5 18.7175 2.5 20.1088 3.89124C21.5 5.28249 21.5 7.52166 21.5 12C21.5 16.4783 21.5 18.7175 20.1088 20.1088C18.7175 21.5 16.4783 21.5 12 21.5C7.52166 21.5 5.28249 21.5 3.89124 20.1088C2.5 18.7175 2.5 16.4783 2.5 12Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M5 21C9.37246 15.775 14.2741 8.88406 21.4975 13.5424"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
        }
    }

    val File: ImageVector by lazy {
        icon("File") {
            addPath(pathData = addPathNodes("M8 7L16 7"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M8 11L12 11"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M13 21.5V21C13 18.1716 13 16.7574 13.8787 15.8787C14.7574 15 16.1716 15 19 15H19.5M20 13.3431V10C20 6.22876 20 4.34315 18.8284 3.17157C17.6569 2 15.7712 2 12 2C8.22877 2 6.34315 2 5.17157 3.17157C4 4.34314 4 6.22876 4 10L4 14.5442C4 17.7892 4 19.4117 4.88607 20.5107C5.06508 20.7327 5.26731 20.9349 5.48933 21.1139C6.58831 22 8.21082 22 11.4558 22C12.1614 22 12.5141 22 12.8372 21.886C12.9044 21.8623 12.9702 21.835 13.0345 21.8043C13.3436 21.6564 13.593 21.407 14.0919 20.9081L18.8284 16.1716C19.4065 15.5935 19.6955 15.3045 19.8478 14.9369C20 14.5694 20 14.1606 20 13.3431Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val View: ImageVector by lazy {
        icon("View") {
            addPath(pathData = addPathNodes("M21.544 11.045C21.848 11.4713 22 11.6845 22 12C22 12.3155 21.848 12.5287 21.544 12.955C20.1779 14.8706 16.6892 19 12 19C7.31078 19 3.8221 14.8706 2.45604 12.955C2.15201 12.5287 2 12.3155 2 12C2 11.6845 2.15201 11.4713 2.45604 11.045C3.8221 9.12944 7.31078 5 12 5C16.6892 5 20.1779 9.12944 21.544 11.045Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M15 12C15 10.3431 13.6569 9 12 9C10.3431 9 9 10.3431 9 12C9 13.6569 10.3431 15 12 15C13.6569 15 15 13.6569 15 12Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
        }
    }

    val ViewOff: ImageVector by lazy {
        icon("ViewOff") {
            addPath(pathData = addPathNodes("M19.439 15.439C20.3636 14.5212 21.0775 13.6091 21.544 12.955C21.848 12.5287 22 12.3155 22 12C22 11.6845 21.848 11.4713 21.544 11.045C20.1779 9.12944 16.6892 5 12 5C11.0922 5 10.2294 5.15476 9.41827 5.41827M6.74742 6.74742C4.73118 8.1072 3.24215 9.94266 2.45604 11.045C2.15201 11.4713 2 11.6845 2 12C2 12.3155 2.15201 12.5287 2.45604 12.955C3.8221 14.8706 7.31078 19 12 19C13.9908 19 15.7651 18.2557 17.2526 17.2526"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M9.85786 10C9.32783 10.53 9 11.2623 9 12.0711C9 13.6887 10.3113 15 11.9289 15C12.7377 15 13.47 14.6722 14 14.1421"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M3 3L21 21"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Coins: ImageVector by lazy {
        icon("Coins") {
            addPath(pathData = addPathNodes("M9,11A6.5,2 0 1,0 22,11A6.5,2 0 1,0 9,11Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M22 15.5C22 16.6046 19.0899 17.5 15.5 17.5C11.9101 17.5 9 16.6046 9 15.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M22 11V19.8C22 21.015 19.0899 22 15.5 22C11.9101 22 9 21.015 9 19.8V11"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M2,4A6.5,2 0 1,0 15,4A6.5,2 0 1,0 2,4Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Butt, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M6 11C4.10819 10.7698 2.36991 10.1745 2 9M6 16C4.10819 15.7698 2.36991 15.1745 2 14"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M6 21C4.10819 20.7698 2.36991 20.1745 2 19L2 4"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
            addPath(pathData = addPathNodes("M15 6V4"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Miter)
        }
    }

    val Wallet: ImageVector by lazy {
        icon("Wallet") {
            addPath(pathData = addPathNodes("M14 3H5C3.89543 3 3 3.89543 3 5C3 6.10457 3.89543 7 5 7H18C18 6.07003 18 5.60504 17.8978 5.22354C17.6204 4.18827 16.8117 3.37962 15.7765 3.10222C15.395 3 14.93 3 14 3Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M3 5V15C3 17.8284 3 19.2426 3.87868 20.1213C4.75736 21 6.17157 21 9 21H15C17.8284 21 19.2426 21 20.1213 20.1213C21 19.2426 21 17.8284 21 15V13C21 10.1716 21 8.75736 20.1213 7.87868C19.2426 7 17.8284 7 15 7H7"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M21 12H19C18.535 12 18.3025 12 18.1118 12.0511C17.5941 12.1898 17.1898 12.5941 17.0511 13.1118C17 13.3025 17 13.535 17 14C17 14.465 17 14.6975 17.0511 14.8882C17.1898 15.4059 17.5941 15.8102 18.1118 15.9489C18.3025 16 18.535 16 19 16H21"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Alert: ImageVector by lazy {
        icon("Alert") {
            addPath(pathData = addPathNodes("M13.9248 21H10.0752C5.44476 21 3.12955 21 2.27636 19.4939C1.42317 17.9879 2.60736 15.9914 4.97574 11.9985L6.90057 8.75333C9.17559 4.91778 10.3131 3 12 3C13.6869 3 14.8244 4.91777 17.0994 8.75332L19.0243 11.9985C21.3926 15.9914 22.5768 17.9879 21.7236 19.4939C20.8704 21 18.5552 21 13.9248 21Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M12 9V13"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M12.125 16.75H12M12.25 16.75C12.25 16.8881 12.1381 17 12 17C11.8619 17 11.75 16.8881 11.75 16.75C11.75 16.6119 11.8619 16.5 12 16.5C12.1381 16.5 12.25 16.6119 12.25 16.75Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Info: ImageVector by lazy {
        icon("Info") {
            addPath(pathData = addPathNodes("M2,12A10,10 0 1,0 22,12A10,10 0 1,0 2,12Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M12 16V12"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M12.125 8.25H12M12.25 8.25C12.25 8.11193 12.1381 8 12 8C11.8619 8 11.75 8.11193 11.75 8.25C11.75 8.38807 11.8619 8.5 12 8.5C12.1381 8.5 12.25 8.38807 12.25 8.25Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val Key: ImageVector by lazy {
        icon("Key") {
            addPath(pathData = addPathNodes("M15.5 14.5C18.8137 14.5 21.5 11.8137 21.5 8.5C21.5 5.18629 18.8137 2.5 15.5 2.5C12.1863 2.5 9.5 5.18629 9.5 8.5C9.5 9.38041 9.68962 10.2165 10.0303 10.9697L2.5 18.5V21.5H5.5V19.5H7.5V17.5H9.5L13.0303 13.9697C13.7835 14.3104 14.6196 14.5 15.5 14.5Z"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            addPath(pathData = addPathNodes("M17.5 6.5L16.5 7.5"), stroke = Ink, strokeLineWidth = 1.5f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }

    val HomeSolid: ImageVector by lazy {
        icon("HomeSolid") {
            addPath(pathData = addPathNodes("M21.75 14.5839V14.5263V11.9892C21.75 10.3816 21.7674 9.3302 21.3174 8.41006C20.8672 7.49013 20.0266 6.85888 18.7578 5.87198L16.7578 4.31631L16.7266 4.29203C15.7233 3.51174 14.9179 2.88528 14.2119 2.45987C13.4812 2.01964 12.7872 1.74991 12 1.74991C11.2127 1.74991 10.5187 2.01964 9.78805 2.45987C9.08203 2.88528 8.27661 3.51173 7.2734 4.29201L7.24216 4.31631L5.24216 5.87198C3.97329 6.85888 3.1327 7.49013 2.68259 8.41006C2.23256 9.3302 2.24997 10.3816 2.24997 11.9892V14.5263L2.24997 14.5839C2.24995 16.1855 2.24993 17.4641 2.38473 18.4667C2.52407 19.5028 2.82001 20.3561 3.49509 21.0312C4.44344 21.9794 5.75522 22.1863 7.46286 22.2489C8.45826 22.2853 9.24985 21.4774 9.24997 20.5116V15.0263C9.24997 14.3359 9.80961 13.7763 10.5 13.7763H13.5C14.1903 13.7763 14.75 14.3359 14.75 15.0263V20.5116C14.7501 21.4774 15.5417 22.2853 16.5371 22.2489C18.2447 22.1863 19.5565 21.9794 20.5049 21.0312C21.1799 20.3561 21.4759 19.5028 21.6152 18.4667C21.75 17.4641 21.75 16.1855 21.75 14.5839Z"), fill = Ink)
        }
    }

    val MessagesSolid: ImageVector by lazy {
        icon("MessagesSolid") {
            addPath(pathData = addPathNodes("M9.77965 1.82273C11.2369 1.72586 12.7601 1.72566 14.2204 1.82273C18.787 2.12629 22.4103 5.81258 22.7082 10.4224C22.7639 11.2848 22.7639 12.1768 22.7082 13.0392C22.4103 17.649 18.787 21.3353 14.2204 21.6389C12.7601 21.7359 11.2369 21.7357 9.77965 21.6389C9.21472 21.6013 8.59978 21.4677 8.05839 21.2448C7.8203 21.1467 7.65868 21.0804 7.54041 21.037C7.45909 21.0929 7.35108 21.1723 7.1938 21.2883C6.40136 21.8726 5.40092 22.2825 3.98117 22.248L3.93544 22.2469C3.66155 22.2403 3.36961 22.2334 3.13152 22.1873C2.84475 22.1318 2.48996 21.9931 2.26791 21.6145C2.02623 21.2025 2.12313 20.7858 2.21688 20.5234C2.30536 20.2757 2.45874 19.9852 2.61542 19.6885L2.6369 19.6478C3.10323 18.7641 3.23314 18.0419 2.98381 17.5604C2.15148 16.304 1.40272 14.7556 1.2918 13.0392C1.23607 12.1768 1.23607 11.2848 1.2918 10.4224C1.58972 5.81258 5.213 2.12629 9.77965 1.82273ZM7.75 9.5C7.75 9.91421 8.08579 10.25 8.5 10.25H12C12.4142 10.25 12.75 9.91421 12.75 9.5C12.75 9.08579 12.4142 8.75 12 8.75H8.5C8.08579 8.75 7.75 9.08579 7.75 9.5ZM7.75 14.5C7.75 14.9142 8.08579 15.25 8.5 15.25H15.5C15.9142 15.25 16.25 14.9142 16.25 14.5C16.25 14.0858 15.9142 13.75 15.5 13.75H8.5C8.08579 13.75 7.75 14.0858 7.75 14.5Z"), fill = Ink, pathFillType = PathFillType.EvenOdd)
        }
    }

    val InboxSolid: ImageVector by lazy {
        icon("InboxSolid") {
            addPath(pathData = addPathNodes("M12.8251 1.75C15.0007 1.74998 15.7354 1.74997 17.0955 1.93282C18.4999 2.12164 19.6537 2.52175 20.5661 3.43414C21.4785 4.34653 21.8786 5.50033 22.0674 6.90471C22.2313 8.12428 22.2483 10.047 22.25 12L22.2436 13.5037C22.235 15.2454 22.1957 16.6539 21.9907 17.7892C21.7817 18.9461 21.3902 19.8839 20.635 20.6391C19.7768 21.4973 18.6846 21.8843 17.3079 22.0694C15.9645 22.25 14.2438 22.25 12.0531 22.25H11.9387C9.74804 22.25 8.02737 22.25 6.68396 22.0694C5.3073 21.8843 4.21505 21.4973 3.35685 20.6391C2.60168 19.8839 2.21018 18.9461 2.00122 17.7892C1.79615 16.6539 1.75684 15.2454 1.74826 13.5037L1.7504 11.9999C1.75213 10.0469 1.76906 8.12426 1.93303 6.90471C2.12184 5.50033 2.52195 4.34653 3.43434 3.43414C4.34673 2.52175 5.50054 2.12164 6.90492 1.93282C8.26505 1.74996 9.99978 1.74998 12.1757 1.75H12.8251ZM20.0852 7.17121C20.2409 8.32894 20.2497 10.2983 20.2502 12.4499C20.2502 12.6155 20.1159 12.75 19.9502 12.75L16.5703 12.75C15.2901 12.75 14.4348 13.7898 14.0243 14.6123C13.7341 15.1938 13.1705 15.75 11.9959 15.75C10.8213 15.75 10.2578 15.1938 9.96755 14.6123C9.55706 13.7898 8.70178 12.75 7.42159 12.75L4.0502 12.75C3.88452 12.75 3.75019 12.6155 3.75023 12.4499C3.75071 10.2983 3.75954 8.32894 3.91519 7.17121C4.07419 5.9886 4.3697 5.3272 4.84855 4.84835C5.32741 4.3695 5.98881 4.07399 7.17141 3.91499C8.38278 3.75212 10.4828 3.75 12.7502 3.75C15.0176 3.75 15.6176 3.75212 16.829 3.91499C18.0116 4.07399 18.673 4.3695 19.1519 4.84835C19.6307 5.3272 19.9262 5.9886 20.0852 7.17121Z"), fill = Ink, pathFillType = PathFillType.EvenOdd)
        }
    }

    val ConnectSolid: ImageVector by lazy {
        icon("ConnectSolid") {
            addPath(pathData = addPathNodes("M7.93707 10.9819C7.50512 10.55 6.96007 10.259 6.34472 10.2502C5.72436 10.2413 5.15975 10.521 4.69883 10.9819L3.97297 11.7078C1.91672 13.764 1.70097 16.9638 3.32572 19.2601L1.54289 21.0429C1.15237 21.4334 1.15237 22.0666 1.54289 22.4571C1.93342 22.8476 2.56658 22.8476 2.95711 22.4571L4.73993 20.6743C7.03616 22.299 10.236 22.0833 12.2922 20.027L13.0181 19.3012C13.45 18.8692 13.741 18.3242 13.7498 17.7088C13.7587 17.0885 13.479 16.5239 13.0181 16.0629L7.93707 10.9819Z"), fill = Ink, pathFillType = PathFillType.EvenOdd)
            addPath(pathData = addPathNodes("M22.4571 2.95711L20.6743 4.73993C22.299 7.03616 22.0833 10.236 20.027 12.2922L19.3012 13.0181C18.8692 13.45 18.3242 13.741 17.7088 13.7498C17.0885 13.7587 16.5239 13.479 16.0629 13.0181L16.0491 13.0042L15.0766 13.9766C14.7121 14.3411 14.1212 14.3411 13.7567 13.9766C13.3922 13.6121 13.3922 13.0212 13.7567 12.6567L14.7291 11.6843L12.3157 9.27087L11.3433 10.2433C10.9788 10.6078 10.3879 10.6078 10.0234 10.2433C9.65888 9.87881 9.65888 9.28786 10.0234 8.92337L10.9958 7.95094L10.9819 7.93707C10.55 7.50512 10.259 6.96007 10.2502 6.34472C10.2413 5.72436 10.521 5.15975 10.9819 4.69883L11.7078 3.97297C13.764 1.91672 16.9638 1.70097 19.2601 3.32572L21.0429 1.54289C21.4334 1.15237 22.0666 1.15237 22.4571 1.54289C22.8476 1.93342 22.8476 2.56658 22.4571 2.95711Z"), fill = Ink, pathFillType = PathFillType.EvenOdd)
        }
    }

    val SettingsSolid: ImageVector by lazy {
        icon("SettingsSolid") {
            addPath(pathData = addPathNodes("M9.74975 22.4469C10.0241 22.6444 10.3181 22.7181 10.5977 22.75H13.4023C13.6819 22.7181 13.9759 22.6444 14.2503 22.4469C14.5245 22.2495 14.6873 21.9946 14.8058 21.7402C14.9122 21.5117 15.4485 19.9112 15.4485 19.9112C15.4485 19.9112 15.7104 19.4678 15.9256 19.3266C16.3481 19.0495 16.7744 18.9894 17.0521 19.0495C17.0521 19.0495 18.9336 19.5792 19.1993 19.6213C19.4948 19.6681 19.8181 19.674 20.1524 19.5385C20.4866 19.4031 20.714 19.1739 20.893 18.935L22.3316 16.4526C22.4442 16.1934 22.5272 15.9008 22.4917 15.5633C22.4561 15.2258 22.3138 14.9568 22.1496 14.7266C22.0021 14.5199 20.7592 13.1363 20.5441 12.898C20.5441 12.898 20.2841 12.4993 20.2841 11.9998C20.2841 11.4151 20.5441 11.1019 20.5441 11.1019C20.5441 11.1019 22.0021 9.47998 22.1496 9.2733C22.3138 9.04311 22.4561 8.7741 22.4917 8.4366C22.5272 8.09914 22.4442 7.80652 22.3316 7.54733C22.2304 7.31456 20.893 5.06487 20.893 5.06487C20.714 4.82595 20.4866 4.59685 20.1524 4.46138C19.8181 4.32586 19.4948 4.33183 19.1993 4.3786C18.9336 4.42064 17.382 4.85698 17.0521 4.95035C17.0521 4.95035 16.5227 5.06487 15.9256 4.67329C15.7104 4.53218 15.5437 4.32813 15.4484 4.08872C15.4484 4.08872 14.9122 2.48829 14.8058 2.25981C14.6873 2.00543 14.5245 1.75047 14.2503 1.55308C13.9759 1.3556 13.6819 1.28188 13.4023 1.25H10.5977C10.3181 1.28188 10.0241 1.3556 9.74975 1.55308C9.47551 1.75047 9.31274 2.00543 9.19424 2.25981C9.0878 2.48829 8.55157 4.08872 8.55157 4.08872C8.45627 4.32813 8.28956 4.53218 8.07438 4.67329C7.47727 5.06487 6.94794 4.95035 6.94794 4.95035C6.61796 4.85698 5.06641 4.42064 4.80069 4.3786C4.50518 4.33183 4.18189 4.32586 3.84759 4.46138C3.51342 4.59685 3.28602 4.82595 3.10699 5.06487C3.10699 5.06487 1.76957 7.31456 1.66843 7.54733C1.55583 7.80652 1.47276 8.09914 1.50833 8.4366C1.5439 8.7741 1.68619 9.04311 1.85043 9.2733C1.99791 9.47998 3.45586 11.1019 3.45586 11.1019C3.45586 11.1019 3.71591 11.4151 3.71591 11.9998C3.71591 12.4993 3.45588 12.898 3.45588 12.898C3.24083 13.1363 1.99791 14.5199 1.85044 14.7266C1.68619 14.9568 1.5439 15.2258 1.50833 15.5633C1.47276 15.9008 1.55583 16.1934 1.66844 16.4526L3.10697 18.935C3.28601 19.1739 3.51341 19.4031 3.84761 19.5385C4.18191 19.674 4.50519 19.6681 4.80071 19.6213C5.06643 19.5792 6.94789 19.0495 6.94789 19.0495C7.22563 18.9894 7.65193 19.0495 8.07443 19.3266C8.28957 19.4678 8.55153 19.9112 8.55153 19.9112C8.55153 19.9112 9.0878 21.5117 9.19424 21.7402C9.31274 21.9946 9.47551 22.2495 9.74975 22.4469ZM12.0195 15.5C13.9525 15.5 15.5195 13.933 15.5195 12C15.5195 10.067 13.9525 8.5 12.0195 8.5C10.0865 8.5 8.51953 10.067 8.51953 12C8.51953 13.933 10.0865 15.5 12.0195 15.5Z"), fill = Ink, pathFillType = PathFillType.EvenOdd)
        }
    }
}
