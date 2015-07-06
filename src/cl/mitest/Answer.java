package cl.mitest;

/**
 * Clase que representa una respuesta para cierta pregunta.
 * 
 * La respuesta puede o no ser correcta, por lo cual se puede decir
 * que es la clase que representa cada una de las alternativas.
 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
 * @version 2012-09-24
 */
public class Answer {

	public String answer; ///< Respuesta
	public boolean isCorrect; ///< Indica si es correcta o no
	
	/**
	 * Constructor de la clase 
	 * @param answer Respuesta que representa el objeto
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-24
	 */
	public Answer (String answer) {
		this.answer = answer;
		this.isCorrect = false;
	}
	
}
