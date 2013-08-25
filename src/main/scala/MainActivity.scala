package com.jack5.palindrompit

import _root_.android.app.Activity
import _root_.android.os.Bundle
import android.widget.{TextView, EditText}
import android.text.{Editable, TextWatcher}
import android.util.Log
import scala.collection.mutable.ArrayBuffer

import scala.actors.Actor

object Logic {
  val allPalCache = scala.collection.mutable.HashMap.empty[Int, ArrayBuffer[Int]]
  val pairsCache = scala.collection.mutable.HashMap.empty[Int, ArrayBuffer[Int]]

  def priceToCents(price: String): Int = {
    val parts = price.split("\\.")
    var ret = 0
    if (parts.length >= 1) {
      ret += 100 * parts(0).toInt
    }
    if (parts.length >= 2) {
      // Treat 1.5 as 1.50
      ret += (parts(1) + "00").slice(0,2).toInt
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

  def allPalindromes(length: Int): ArrayBuffer[Int] = {
    if (allPalCache.contains(length)) {
      return allPalCache(length)
    }

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
    allPalCache(length) = ret.sorted
    return allPalCache(length)
  }

  def palindromePairs(x: Int): ArrayBuffer[Int] = {
    if (pairsCache.contains(x)) {
      return pairsCache(x)
    }
    var pairs = ArrayBuffer[Int]()
    for (length <- 1 to x.toString.length) {
      for (pal <- allPalindromes(length)) {
        if (isPalindrome(x + pal)) {
          pairs.append(pal)
        }
      }
    }
    pairsCache(x) = pairs
    return pairs
  }

  def outputFromInput(input: String): String = {
    val cents = priceToCents(input)
    val pairs = palindromePairs(cents)
    var text = ""
    for (pair <- pairs) {
      val total = cents + pair
      val percentage = pair * 100 / cents
      text += percentage + "% " + formatPrice(pair) + " " + formatPrice(total) + "\n"
    }
    text
  }
}

class PalindromeWorker(mainActivity: MainActivity) extends Actor {
  def act() {
    val price = mainActivity.input.getText().toString()
    val output = Logic.outputFromInput(price)
    mainActivity.runOnUiThread(new Runnable {
      def run() {
        mainActivity.output.setText(output)
      }
    })
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

    val thisActivity = this
    input.addTextChangedListener(new TextWatcher {
      def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        output.setText("")
        if (input.getText.toString == "") {
          return
        }

        val actor = new PalindromeWorker(thisActivity)
        actor.start
      }
      def afterTextChanged(s: Editable) {}
      def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    })
  }
}
