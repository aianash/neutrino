package neutrino.service

import scaldi.Module
import scaldi.Injectable._

package object injectors {
  class NeutrinoServiceModule extends Module {
    bind [NeutrinoService] to new NeutrinoService
  }
}