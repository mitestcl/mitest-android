package cl.mitest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Clase para cargar y manejar una prueba
 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
 * @version 2012-09-25
 */
public class Test {
	
	public int corrects; ///< Preguntas respondidas de forma correcta
	public String category; ///< Categoría de la prueba
	public String title; ///< Título de la prueba
	public String author; ///< Autor de la prueba
	public String created; ///< Fecha y hora de creación
	public String modified; ///< Fecha y hora de última modificación
	public String generated; ///< Fecha y hora en que el archivo cargado fue generado
	public ArrayList<Question> questions; ///< Listado de preguntas de la prueba
	private String location; ///< Directorio padre del archivo que se usó para cargar la prueba
	private boolean zip; ///< Verdadero si el test que se cargó es un archivo zip
	private Map<String, InputStream> images;
	private static String DS = System.getProperty("file.separator");

	/**
	 * Constructor de la clase 
	 * @param file Ruta del archivo que contiene la prueba que se desea cargar
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-24
	 */
	public Test (String file) {
		// valores por defecto
		this.corrects = 0;
		this.category = "";
		this.title = "";
		this.author = "";
		this.created = "";
		this.modified = "";
		this.generated = "";
		this.questions = new ArrayList<Question>();
		this.location = (new File(file)).getParent();
		this.zip = false;
		this.images = new HashMap<String, InputStream>();
		// cargar test
		this.load(file);
		// mezclar preguntas
		Collections.shuffle(this.questions, new Random(System.nanoTime()));
	}
	
	/**
	 * Método que realiza la carga de la prueba
	 * 
	 * Este método entrega una capa de abstracción para la carga de archivos,
	 * ya que permite cargar archivos en diferentes formatos (mt, xml y json)
	 * @param file Ruta del archivo que contiene la prueba que se desea cargar
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-24
	 */
	private void load (String file) {
		if(file.endsWith("zip")) this.loadZIP(file);
		else if(file.endsWith("mt")) this.loadMT(file);
		else if(file.endsWith("xml")) this.loadXML(file);
		else if(file.endsWith("json")) this.loadJSON(file);
	}
	
	/**
	 * Método que carga la prueba desde un archivo .zip
	 * @param file Ruta del archivo que contiene la prueba que se desea cargar
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-25
	 */
	private void loadZIP (String file) {
		Enumeration<? extends ZipEntry> entries;
	    ZipFile zipFile;
	    ZipEntry entry;
	    this.zip = true;
	    boolean loaded = false;
	    try {
	    	zipFile = new ZipFile(file);
	    	entries = zipFile.entries();
	    	entry = (ZipEntry)entries.nextElement();
	    	// procesar entradas del archivo zip
	    	while(entries.hasMoreElements()) {
	    		// obtener entrada
	    		entry = (ZipEntry)entries.nextElement();
	    		// si existe un archivo mt en el zip
	    		if(entry.getName().endsWith("mt")) {
	    			if(!loaded) {
	    				this.loadMT(zipFile.getInputStream(entry));
	    				loaded = true;
	    			}
	    		}
	    		// si existe un archivo json en el zip
	    		else if(entry.getName().endsWith("json")) {
	    			if(!loaded) {
	    				this.loadJSON(zipFile.getInputStream(entry));
	    				loaded = true;
	    			}
	    		}
	    		// si existe un archivo xml en el zip
	    		else if(entry.getName().endsWith("xml")) {
	    			if(!loaded) {
	    				this.loadXML(zipFile.getInputStream(entry));
	    				loaded = true;
	    			}
	    		}
	    		// si es una imagen
	    		else if(entry.getName().contains(DS+"img"+DS) && !entry.getName().endsWith("/")) {
	    			File imageFile = new File(entry.getName());
	    			this.images.put(imageFile.getName(), zipFile.getInputStream(entry));
	    		}
	    	}
	    	//zipFile.close(); // no se puede cerrar, ya que si se cierra se pierden los InputStream de this.images
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	}
	
	/**
	 * Método que carga la prueba desde un archivo .mt
	 * @param file Ruta del archivo que contiene la prueba que se desea cargar
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-25
	 */
	private void loadMT (String file) {
		try {
			this.loadMT(new FileInputStream(new File(file)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Método que carga la prueba desde un InputStream de un archivo .mt
	 * @param is InputStream del archivo a cargar
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-25
	 */
	private void loadMT (InputStream is) {
		// abrir archivo y obtener buffer
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
   		// procesar cada una de las líneas
		String line;
		Question question = null;
		ArrayList<Answer> answers = null;
		try {
    		while((line = in.readLine())!= null) {
    			// si la línea esta vacía o parte con un # se omite
    			if (!line.startsWith("#") && !line.equals("")) {
    				// obtener información del test
    				if(line.startsWith("!:")) {
    					String[] info = (line.replace("!:", "")).split(":");
    					this.title = info[0];
    					if(info.length>1) this.category = info[1];
    					if(info.length>2) this.author = info[2];
    				}
    				// si no es información del test se asume que es una pregunta
    				else {
    					// separar la línea en dos partes, si la primera parte
    					// es un número se está procesando una nueva pregunta
    					// las alternativas son:
    					// numero pregunta / pregunta
    					// letra alternativa / alternativa
    					// (T) / tipo (easy, normal, hard)
    					// (I) / imagen
    					// (E) / explicacion
    					String[] aux = line.split(" ", 2);
    					// es numero pregunta / pregunta
    					if(this.isInt(aux[0])) {
    						// crear pregunta y alternativas
    						question = new Question(aux[1]);
    						answers = new ArrayList<Answer>();
    					}
    					// si es (T) / tipo
    					else if(aux[0].equals("(T)")) {
    						question.type = aux[1];
    					}
    					// si es (I) / imagen
    					else if(aux[0].equals("(I)")) {
    						question.image = this.imageSearch(aux[1]);
    					}
    					// si es (E) / explicacion
    					// esto es lo último que aparece en una pregunta
    					// con esto se sabe que la pregunta ya terminó
    					else if (aux[0].equals("(R)") || aux[0].equals("(E)")) {
    						// agregar explicacion
    						question.reason = aux[1];
    						// agregar alternativas a la pregunta
    						question.answers = answers;
    						// mezclar respuestas
    						Collections.shuffle(question.answers, new Random(System.nanoTime()));
    						// agregar pregunta al arreglo
    						this.questions.add(question);
    						// resetear pregunta y alternativas para pasar a la siguiente
    						question = null;
    						answers = null;
    					}
    					// si no es ninguna de las anteriores es una alternativa
    					else {
    						// crear alternativa
    						Answer answer = new Answer(aux[1]);
    						// si es la alternativa correcta
    						if(aux[0].startsWith("*")) {
    							answer.isCorrect = true;
    							question.corrects++;
    						}
    						// agregar al listado de alternativas
    						answers.add(answer);
    					}
    				}
    			}
    		}
    		in.close();
    	} catch (IOException e){
    		e.printStackTrace();
    	}
	}
	
	/**
	 * Clase para mapeo desde JSON
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-24
	 */
	private class PruebaJSON {
		public String categoria;
		public String prueba;
		public String autor;
		public String generada;
		public String creada;
		public String modificada;
		ArrayList<PreguntaJSON> preguntas;
	}
	
	/**
	 * Clase para mapeo desde JSON
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-24
	 */
	private class PreguntaJSON {
		public String tipo;
		public String pregunta;
		public String imagen;
		public String explicacion;
		ArrayList<AlternativaJSON> alternativas;
	}
	
	/**
	 * Clase para mapeo desde JSON
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-24
	 */
	private class AlternativaJSON {
		public boolean correcta;
		public String alternativa;
	}

	/**
	 * Método que carga la prueba desde un InputStream de un archivo .json
	 * @param is InputStream del archivo a cargar
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-25
	 */
	private void loadJSON (InputStream is) {
		// variable para resultado json
		String json = "";
		// abrir archivo y obtener buffer
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		// procesar cada una de las líneas
		String line;
		try {
			while((line = in.readLine())!= null) {
				json += line;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// procesar JSON
		this.loadJSON(json);
	}
	
	/**
	 * Método que carga la prueba desde un archivo .json
	 * @param file Ruta del archivo que contiene la prueba que se desea cargar
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-24
	 */
	private void loadJSON (String file) {
		// leer string json
		String json = null;
		// si file inicia con / es una ruta
		if(file.startsWith("/")) {
			try {
				json = this.readFile(file);
			} catch (IOException e) {}
		}
		// si no inicia con / es el archivo json ya listo (¡terrible parche!)
		else {
			json = file;
		}
		// si se leyó el archivo se procesa
		if(json!=null) {
			// transformar de json a objetos
			Gson gson = new GsonBuilder().create();
			PruebaJSON prueba = gson.fromJson(json, PruebaJSON.class);
			// si se pudo obtener el objeto se procesa
			if(prueba!=null) {
				// cargar cabecera
				this.category = prueba.categoria;
				this.title = prueba.prueba;
				this.author = prueba.autor;
				this.created = prueba.creada;
				this.modified = prueba.modificada;
				this.generated = prueba.generada;
				// cargar preguntas
				for(int i=0; i<prueba.preguntas.size(); ++i) {
					// obtener pregunta desde JSON
					PreguntaJSON pregunta = (PreguntaJSON) prueba.preguntas.get(i);
					// crear pregunta y alternativas
					Question question = new Question(pregunta.pregunta);
					question.type = pregunta.tipo;
					question.reason = pregunta.explicacion;
					// si se indico una imagen se busca
					if(pregunta.imagen!=null && !pregunta.imagen.equals("")) {
						question.image = this.imageSearch(pregunta.imagen);
					}
					// procesar alternativas
					question.answers = new ArrayList<Answer>();
					for(int j=0; j<pregunta.alternativas.size(); ++j) {
						// recuperar alternativa desde json
						AlternativaJSON alternativa = (AlternativaJSON) pregunta.alternativas.get(j);
						// crear alternativa
						Answer answer = new Answer(alternativa.alternativa);
						// si es la alternativa correcta
						if(alternativa.correcta) {
							answer.isCorrect = true;
							question.corrects++;
						}
						// agregar al listado de alternativas
						question.answers.add(answer);
					}
					// mezclar respuestas
					Collections.shuffle(question.answers, new Random(System.nanoTime()));
					// agregar pregunta al arreglo
					this.questions.add(question);
				}
			}
		}
	}
	
	/**
	 * Método que carga la prueba desde un archivo .xml
	 * @param file Ruta del archivo que contiene la prueba que se desea cargar
	 * @todo Implementar método
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2013-08-03
	 */
	private void loadXML (String file) {
		try {
			this.loadXML(new FileInputStream(new File(file)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Método que carga la prueba desde un InputStream de un archivo .xml
	 * @param is InputStream del archivo a cargar
	 * @todo Implementar método
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2013-08-03
	 */
	private void loadXML (InputStream is) {
		// procesar XML
		XML xml = new XML(is);
		Document dom = xml.getDom();
		// cargar cabecera
		this.category = xml.getFirstNodeValue("categoria");
		this.title = xml.getFirstNodeValue("prueba");
		this.author = xml.getFirstNodeValue("autor");
		this.created = xml.getFirstNodeValue("creada");
		this.modified = xml.getFirstNodeValue("modificada");
		this.generated = xml.getFirstNodeValue("generada");
		// cargar preguntas
		NodeList preguntas = dom.getElementsByTagName("question");
		for (int i=0; i<preguntas.getLength(); i++) {
			Node pregunta = preguntas.item(i);
			NodeList datosPregunta = pregunta.getChildNodes();
			Question question = new Question("");
			for (int j=0; j<datosPregunta.getLength(); j++) {
                 Node dato = datosPregunta.item(j);
                 String tag = dato.getNodeName();
                 if (tag.equals("tipo")) {
                	 question.type = xml.getNodeValue(dato);
                 } else if (tag.equals("pregunta")) {
                	 question.question = xml.getNodeValue(dato);
                 } else if (tag.equals("imagen")) {
                	 question.image = this.imageSearch(xml.getNodeValue(dato));
                 } else if (tag.equals("explicacion")) {
                	 question.reason = xml.getNodeValue(dato);
                 } else if (tag.equals("alternativas")) {
                	 question.answers = new ArrayList<Answer>();
                	 NodeList alternativas = dato.getChildNodes();
                	 for (int k=0; k<alternativas.getLength(); k++) {
                		 // recuperar alternativa
                		 Node alternativa = alternativas.item(k);
                		 NodeList datosAlternativa = alternativa.getChildNodes();
                		 Answer answer = new Answer("");
                		 for (int l=0; l<datosAlternativa.getLength(); l++) {
                			 Node d = datosAlternativa.item(l);
                             String t = d.getNodeName();
                             if (t.equals("correcta")) {
                            	 if(xml.getNodeValue(d).equals("si")) {
                            		 answer.isCorrect = true;
                        			 question.corrects++;
                            	 }
                             } else if (t.equals("alternativa")) {
                            	 answer.answer = xml.getNodeValue(d);
                             }
                		 }
                		 // agregar al listado de alternativas
                		 if(!answer.answer.equals(""))
                			 question.answers.add(answer);
                	 }
                 }
            }
			// mezclar respuestas
			Collections.shuffle(question.answers, new Random(System.nanoTime()));
			// agregar pregunta al arreglo
			this.questions.add(question);
		}
	}

	public String imageSearch (String imagen) {
		// si se indico una imagen se busca
   	 	if(imagen!=null && !imagen.equals("")) {
   	 		// si se carga un archivo zip no se busca la imagen
   	 		// solo se asigna el nombre que viene
   	 		if(this.zip) {
   	 			return imagen;
   	 		}
   	 		// si no es zip, si la imagen existe se agrega
   	 		else {
   	 			// si la imagen existe se agrega
   	 			String image = this.location+DS+"img"+DS+imagen;
   	 			if((new File(image)).exists()) {
   	 				return image;
   	 			}
   	 		}
   	 	}
   	 	return null;
	}

	/**
	 * Método que entrega la cantidad de preguntas que tiene la prueba
	 * @return Integer Cantidad de preguntas que tiene la prueba
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-24
	 */
	public int questions () {
		return this.questions.size();
	}
	
	/**
	 * Método que entrega el objeto que representa una pregunta
	 * @param i Número de la pregunta que se desea recuperar
	 * @return Question Objeto con la pregunta (o null en caso de error)
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-24
	 */
	public Question question (int n) {
		// si n es menor o igual que el tamaño del arreglo se retorna la pregunta
		if(n<=this.questions.size())
			return (Question) this.questions.get(n-1);
		// si está fuera de rango se retorna nulo
		else
			return null;
	}
	
	/**
	 * Método que entrega una imagen desde el mapa de imagenes y sus InputStream
	 * @param image Nombre de la imagen que se busca
	 * @return InputStream InputStream de la imagen
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-25
	 */
	public InputStream getImage(String image) {
		return (InputStream)this.images.get(image);
	}
	
	/** TODO: separar a otro fichero los métodos de abajo */
	
	/**
	 * Método que verifica si un String corresponde a un números
	 * @param s Texto que se quiere verificar si representa a un número
	 * @return boolean Verdadero si el string representa un número
	 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
	 * @version 2012-09-24
	 */
	private boolean isInt(String s){
        try {
            Integer.parseInt(s);
            return true;
        } catch (Exception e){
            return false;
        }
    }
	
	/**
	 * Método que lee un archivo de texto completo y lo devuelve en un String
	 * @param path Ruta del archivo que contiene la prueba que se desea cargar
	 * @return String Contenido del archivo leído
	 * @author http://stackoverflow.com/a/326440
	 * @version 2008-11-28
	 */
	private String readFile (String path) throws IOException {
		FileInputStream stream = new FileInputStream(new File(path));
		try {
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
			return Charset.defaultCharset().decode(bb).toString();
		}
		finally {
			stream.close();
		}
	}

}
