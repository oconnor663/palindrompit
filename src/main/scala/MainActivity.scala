package com.jack5.palindrompit

import _root_.android.app.Activity
import _root_.android.os.Bundle
import android.widget.{TextView, EditText}
import android.text.{Editable, TextWatcher}
import android.util.Log
import scala.collection.mutable.ArrayBuffer

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

  def allPalindromes(length: Int) = {
    val ret = new ArrayBuffer[Int]
    val partLen = (length + 1) / 2
    val evenLen = (length % 2 == 0)
    for (part <- 1 until math.pow(10, partLen).toInt) {
      if (part % 10 != 0) {
        val s = ("%0" + partLen + "d").format(part)
        var palindrome = 0
        if (evenLen) {
          palindrome = (s.reverse + s).toInt
        }
        else {
          palindrome = (s.reverse.slice(0, partLen-1) + s).toInt
        }
        ret.append(palindrome)
      }
    }
    ret.sorted
  }

  def palindromePairs(x: Int): ArrayBuffer[Int] = {
    var pairs = ArrayBuffer[Int]()
    for (length <- 1 to x.toString.length) {
      for (pal <- allPalindromes(length)) {
        if (isPalindrome(x + pal)) {
          pairs.append(pal)
        }
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
