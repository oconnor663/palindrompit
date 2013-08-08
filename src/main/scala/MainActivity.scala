package com.jack5.palindrompit

import _root_.android.app.Activity
import _root_.android.os.Bundle
import android.widget.{TextView, EditText}
import android.text.{Editable, TextWatcher}
import android.util.Log

object Logic {
  def priceToCents(price: String): Int = {
    val parts = price.split("\\.")
    var ret = 0
    if (parts.length >= 1) {
      ret += 100 * parts(0).toInt
    }
    if (parts.length >= 2) {
      ret += parts(1).toInt
    }
    ret
  }

  def formatPrice(cents: Int): String = {
    "$%d.%02d".format(cents/100, cents%100)
  }

  def isPalindrome(x: Int): Boolean = {
    val s = x.toString
    s == s.reverse
  }

  def palindromePairs(x: Int): Vector[Int] = {
    var pairs = Vector[Int]()
    for (pair <- 1 to x) {
      if (isPalindrome(pair) && isPalindrome(x + pair)) {
        pairs :+= pair
      }
    }
    pairs
  }
}

class MainActivity extends Activity with TypedActivity {

  var input : EditText= null
  var output : TextView = null

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.main)

    input = findView(TR.input)
    output = findView(TR.output)

    input.addTextChangedListener(new TextWatcher {
      def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        val price = input.getText().toString()
        if (price == "") {
          return
        }
        val cents = Logic.priceToCents(price)
        val pairs = Logic.palindromePairs(cents)
        var text = ""
        for (pair <- pairs) {
          val total = cents + pair
          val percentage = pair * 100 / cents
          text += percentage + "% " + Logic.formatPrice(pair) + " " + Logic.formatPrice(total) + "\n"
        }
        output.setText(text)
        Log.e("JACK", text)
      }
      def afterTextChanged(s: Editable) {}
      def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    })
  }
}
