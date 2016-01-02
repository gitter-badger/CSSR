package com.typeclassified.hmm.cssr.cli

import java.io.File

object Parser {
  val cssr     = "cssr"
  val version  = "v0.1.0"

  // Required
  val alphabet = "alphabet"
  val data     = "data"

  // Optional
  val lMax        = "lMax"
  val lMaxDefault = 5
  val sig         = "sig"
  val sigDefault  = 0.05

  val file = "[file]"
  val value = "[value]"

  val parser = new scopt.OptionParser[Config](Parser.cssr) {
    head(Parser.cssr, Parser.version)

    opt[File](Parser.alphabet.head, Parser.alphabet)
      .required()
      .valueName(file)
      .action { (x, c) => c.copy(alphabetFile = x) }
      .text(s"Required. The $alphabet file for the given data")

    opt[File](Parser.data.head, Parser.data)
      .required()
      .valueName(file)
      .action { (x, c) => c.copy(dataFile = x) }
      .text(s"Required. The $data file of observed sequence")

    opt[Int](Parser.lMax.head, Parser.lMax)
      .valueName(value)
      .action { case (x, c) => c.copy(lMax = x) }
      .validate { x => if (x > 0) success else failure(s"Value <$lMax> must be > 0") }
      .text(s"$lMax is the maximum size of a history. Defaults to $lMaxDefault")

    opt[Double](Parser.sig.head, Parser.sig)
      .valueName(value)
      .action { (x,c) => c.copy(sig = x) }
      .validate { x => if (x > 0 && x < 1) success else failure(s"Value <$sig> must be > 0 && < 1") }
      .text(s"$sig is the significance level used for hypothesis testing. Defaults to $sigDefault")
  }
}
