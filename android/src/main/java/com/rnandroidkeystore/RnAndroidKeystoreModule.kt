package com.rnandroidkeystore

import com.facebook.react.bridge.ReactApplicationContext

class RnAndroidKeystoreModule(reactContext: ReactApplicationContext) :
  NativeRnAndroidKeystoreSpec(reactContext) {

  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }

  companion object {
    const val NAME = NativeRnAndroidKeystoreSpec.NAME
  }
}
