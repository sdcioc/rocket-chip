package freechips.rocketchip.system

import Chisel._
import freechips.rocketchip.config.Config
import freechips.rocketchip.subsystem._
import freechips.rocketchip.diplomacy._

class DefaultConfigRBB extends Config(
new WithJtagDTMSystem ++ new WithNBigCores(1) ++ new BaseConfig)
