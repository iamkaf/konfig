plugins {
    id("dev.kikugie.stonecutter")
    id("com.iamkaf.amber.loom-remap") apply false
    id("com.iamkaf.amber.loom") apply false
    id("com.iamkaf.multiloader.root")
}

stonecutter active "26.3".let { multiloaderStonecutter.active(it) }
