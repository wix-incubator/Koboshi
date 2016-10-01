package org.specs2

import org.specs2.specification.core.Fragment
import org.specs2.specification.dsl.mutable.BlockCreation

trait DescribeOf extends BlockCreation {
  implicit class describeOf(d: String) {
    def of(f: => Fragment): Fragment = addBlock(s"$d of", f, addFragmentBlock)
  }

}
