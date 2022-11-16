package it.prova.gestionesatelliti.web.controller;

import java.util.Date;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.prova.gestionesatelliti.model.Satellite;
import it.prova.gestionesatelliti.model.StatoSatellite;
import it.prova.gestionesatelliti.service.SatelliteService;

@Controller
@RequestMapping(value = "/satellite")
public class SatelliteController {

	@Autowired
	private SatelliteService satelliteService;

	@GetMapping
	public ModelAndView listAll() {
		ModelAndView mv = new ModelAndView();
		List<Satellite> results = satelliteService.listAllElements();
		mv.addObject("satellite_list_attribute", results);
		mv.setViewName("satellite/list");
		return mv;
	}

	@GetMapping("/insert")
	public String create(Model model) {
		model.addAttribute("insert_satellite_attr", new Satellite());
		return "satellite/insert";
	}

	@PostMapping("/save")
	public String save(@Valid @ModelAttribute("insert_satellite_attr") Satellite satellite, BindingResult result,
			RedirectAttributes redirectAttrs) {

		if (result.hasErrors())
			return "satellite/insert";


			if (satellite.getDataLancio()!= null && satellite.getDataLancio().after(new Date()) && satellite.getStato() != null) {
				result.rejectValue("stato", "Satellite.stato.mustBeNullIfDataLancioGreaterThanToday");
				return "satellite/insert";
			}

			if (satellite.getDataLancio()!= null && satellite.getDataRientro()!= null && satellite.getDataLancio().after(satellite.getDataRientro())) {
				result.rejectValue("dataRientro", "Satellite.dataRientro.mustNotBeBeforeDataLancio");
				return "satellite/insert";
			}

			if (satellite.getDataLancio()!= null && satellite.getDataLancio().before(new Date()) && satellite.getStato() == null) {
				result.rejectValue("stato", "Satellite.stato.mustNotBeNull");
				return "satellite/insert";
			}

			if ((satellite.getDataRientro()!= null && satellite.getStato() == StatoSatellite.IN_MOVIMENTO || satellite.getStato() == StatoSatellite.FISSO)
					&& satellite.getDataRientro().before(new Date())) {
				result.rejectValue("stato", "Satellite.stato.ifDataRientroBeforeNowStatoMustBeDisattivato");
				return "satellite/insert";
			}
		

		satelliteService.inserisciNuovo(satellite);

		redirectAttrs.addFlashAttribute("successMessage", "Operazione eseguita correttamente");
		return "redirect:/satellite";
	}

	@GetMapping("/show/{idSatellite}")
	public String show(@PathVariable(required = true) Long idSatellite, Model model) {
		model.addAttribute("show_satellite_attr", satelliteService.caricaSingoloElemento(idSatellite));
		return "satellite/show";
	}

	@GetMapping("/search")
	public String search() {
		return "satellite/search";
	}

	@PostMapping("/list")
	public String listByExample(Satellite example, ModelMap model) {
		List<Satellite> results = satelliteService.findByExample(example);
		model.addAttribute("satellite_list_attribute", results);
		return "satellite/list";
	}

	@GetMapping("/delete/{idSatellite}")
	public String delete(@PathVariable(required = true) Long idSatellite, Model model) {
		model.addAttribute("delete_satellite_attr", satelliteService.caricaSingoloElemento(idSatellite));
		return "satellite/delete";
	}

	@PostMapping("/saveDelete")
	public String saveDelete(@RequestParam Long idSatellite, RedirectAttributes redirectAttrs) {

		Satellite satelliteReloaded = satelliteService.caricaSingoloElemento(idSatellite);

		if (satelliteReloaded.getStato() == StatoSatellite.FISSO
				|| satelliteReloaded.getStato() == StatoSatellite.IN_MOVIMENTO) {
			redirectAttrs.addFlashAttribute("errorMessage", "Impossibile eliminare satellite.");
			return "redirect:/satellite";
		}

		satelliteService.rimuovi(idSatellite);

		redirectAttrs.addFlashAttribute("successMessage", "Operazione eseguita correttamente");
		return "redirect:/satellite";
	}

	@GetMapping("/update/{idSatellite}")
	public String update(@PathVariable(required = true) Long idSatellite, Model model) {
		model.addAttribute("update_satellite_attr", satelliteService.caricaSingoloElemento(idSatellite));
		return "satellite/update";
	}

	@PostMapping("/saveUpdate")
	public String saveUpdate(@Valid @ModelAttribute("update_satellite_attr") Satellite satellite, BindingResult result,
			RedirectAttributes redirectAttrs) {

		if (result.hasErrors())
			return "satellite/update";


			if (satellite.getDataLancio()!= null && satellite.getDataRientro()!= null && satellite.getDataLancio().after(satellite.getDataRientro())) {
				result.rejectValue("dataRientro", "Satellite.dataRientro.mustNotBeBeforeDataLancio");
				return "satellite/update";
			}

			if (satellite.getDataLancio()!= null && satellite.getDataRientro()!= null && satellite.getDataLancio().before(new Date()) && satellite.getDataRientro().before(new Date())
					&& satellite.getStato() != StatoSatellite.DISATTIVATO) {
				result.rejectValue("stato", "Satellite.stato.ifSatelliteAtterratoItMustBeDisattivato");
				return "satellite/update";
			}

			if (satellite.getDataRientro()!= null && satellite.getStato() == StatoSatellite.DISATTIVATO && satellite.getDataRientro() == null) {
				result.rejectValue("dataRientro", "Satellite.dataRientro.ifStatoDisattivatoDataRientoMustBeNotNull");
				return "satellite/update";
			}

			if (satellite.getDataLancio()!= null && satellite.getDataRientro()!= null && satellite.getDataLancio() == null && satellite.getDataRientro() != null) {
				result.rejectValue("dataRientro", "Satellite.dataRientro.dataLancioMustNotBeNullIfDataRientroIsNull");
				return "satellite/update";
			}
		

		satelliteService.aggiorna(satellite);

		redirectAttrs.addFlashAttribute("successMessage", "Operazione eseguita correttamente");
		return "redirect:/satellite";
	}

	@PostMapping("/launch")
	public String launchSatellite(@RequestParam Long idSatellite, RedirectAttributes redirectAttrs) {

		Satellite satelliteToBeUpdated = satelliteService.caricaSingoloElemento(idSatellite);
		satelliteToBeUpdated.setDataLancio(new Date());
		satelliteToBeUpdated.setStato(StatoSatellite.IN_MOVIMENTO);
		satelliteService.aggiorna(satelliteToBeUpdated);

		redirectAttrs.addFlashAttribute("successMessage", "Operazione eseguita correttamente");
		return "redirect:/satellite";
	}

	@PostMapping("/return")
	public String returnSatellite(@RequestParam Long idSatellite, RedirectAttributes redirectAttrs) {

		Satellite satelliteToBeUpdated = satelliteService.caricaSingoloElemento(idSatellite);
		if (satelliteToBeUpdated.getDataLancio() != null && satelliteToBeUpdated.getDataLancio().after(new Date())) {
			redirectAttrs.addFlashAttribute("errorMessage",
					" Impossibile far rientrare. la data di lancio e' successiva a quella odierna.");
			return "redirect:/satellite";
		}
		satelliteToBeUpdated.setDataRientro(new Date());
		satelliteToBeUpdated.setStato(StatoSatellite.DISATTIVATO);
		satelliteService.aggiorna(satelliteToBeUpdated);

		redirectAttrs.addFlashAttribute("successMessage", "Operazione eseguita correttamente");
		return "redirect:/satellite";
	}

	@GetMapping("/cercaSatellitiDaDueAnniInOrbitaNonDisattivati")
	public ModelAndView launchedByMoreThanTwoYears() {
		ModelAndView mv = new ModelAndView();
		List<Satellite> results = satelliteService.cercaTuttiLanciatiDaPiuDiDueAnniENonDisattivati();
		mv.addObject("satellite_list_attribute", results);
		mv.setViewName("satellite/list");
		return mv;
	}

	@GetMapping("/cercaSatellitiDisattivatiMaNonRientrati")
	public ModelAndView launchedAndNeverComeBack() {
		ModelAndView mv = new ModelAndView();
		List<Satellite> results = satelliteService.cercaTuttiByStatoLikeAndDataRientroIsNull();
		mv.addObject("satellite_list_attribute", results);
		mv.setViewName("satellite/list");
		return mv;
	}

	@GetMapping("/cercaSatellitiInOrbitaDaDieciAnniMaFissi")
	public ModelAndView launchedTenYearsAgoAndFissi() {
		ModelAndView mv = new ModelAndView();
		List<Satellite> results = satelliteService.cercaTuttiByStatoLikeAndDataRientroIsNullAndDataLancioBeforeThan();
		mv.addObject("satellite_list_attribute", results);
		mv.setViewName("satellite/list");
		return mv;
	}

}
