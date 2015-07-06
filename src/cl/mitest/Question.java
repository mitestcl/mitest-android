package cl.mitest;

import java.util.ArrayList;

/**
 * Clase que representa una pregunta de una prueba
 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
 * @version 2012-09-24
 */
public class Question {
	
	public String question; ///< Pregunta
	public ArrayList<Answer> answers; ///< Listado de alternativas (o respuestas)
	public String image; ///< Ruta de la imagen o null si no hay una
	public String reason; // Explicación de la(s) respuesta(s) correcta(s)
	public String type; // Tipo de pregunta (Fácil, Normal, Difícil)
	public boolean checked; ///< Indica si la pregunta ya fue revisada
	public int corrects;

	/**
	 * Constructor de la clase
	 * @param question Pregunta que representa el objeto
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-24
	 */
	public Question (String question) {
		this.question = question;
		this.answers = null;
		this.image = null;
		this.reason = "";
		this.type = "";
		this.corrects = 0;
	}
	
	/**
	 * Entrega la cantidad de respuestas correctas que tiene la pregunta
	 * @return Integer Cantidad de respuestas correctas que tiene la pregunta
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-24
	 */
	public int answersCorrect () {
		return this.corrects;
	}
	
	/**
	 * Verifica si la respuesta indicada es o no correcta
	 * @return boolean Verdadero si la respuesta es correcta
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-24
	 */
	public boolean answerIsCorrect(int n) {
		if(this.answers.get(n-1).isCorrect) return true;
		else return false;
	}
	
}
