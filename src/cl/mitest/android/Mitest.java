package cl.mitest.android;

import cl.mitest.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Clase principal de la aplicación MiTeSt para Android
 * @author Esteban De La Fuente Rubio, DeLaF (esteban[at]delaf.cl)
 * @version 2012-09-24
 */
public class Mitest extends Activity {

	private Test test; ///< Prueba que se está ejecutando
	private int currentQuestion; ///< Pregunta actual que se está revisando
	private ArrayList<CheckBox> currentCheckboxes; ///< Listado de checkboxes para la pregunta actual
	private ArrayList<TextView> currentCheckboxesTextViews; ///< Listado de alternativas para la pregunta actual
	private String app_data_path = null;
	
	private static final int ACTIVITY_FILEMANAGER = 1;
	private static final int ACTIVITY_BARCODE_SCANNER = 2;
	
    /**
     * Método que es ejecutado al lanzar la aplicación
     * @todo Programar método para cargar en spinner los tests que estén en
     * directorio de datos
     */
    public void onCreate(Bundle savedInstanceState) {
    	// recomendación ya que se está sobreescribiendo un método de la clase padre Activity
        super.onCreate(savedInstanceState);
        // cargar pantalla principal
        setContentView(R.layout.activity_mitest);
        // cargar pruebas en directorio de la aplicación
        //this.populateLocalTests(this.getLocalTests()); // TODO: en desarrollo
    }

    /**
     * Método para el menú de la aplicación
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_mitest, menu);
        return true;
    }
    
    /**
     * Acción a realizar cuando se haga click en el botón que revisa la pregunta
     */
    private OnClickListener btnCheckListener = new OnClickListener() {
		public void onClick(View v) {
			checkAnswers();
		}
	};
    
	/**
	 * Acción a realizar cuando se haga click en el botón que muestra la explicación de la pregunta
	 */
	private OnClickListener btnReasonListener = new OnClickListener() {
		public void onClick(View v) {
			showReason();
		}
	};
	
	/**
	 * Acción a realizar cuando se haga click en el botón que pasa a la siguiente pregunta
	 */
	private OnClickListener btnNextListener = new OnClickListener() {
		public void onClick(View v) {
			nextQuestion();
		}
	};
	
	/**
	 * Método que revisa las alternativas seleccionadas para una pregunta
	 */
	private void checkAnswers () {
		// variables
		boolean correct = true; // se asume correcta la pregunta
		int selected = 0; // variables para contar los checkboxes seleccionados
		// revisar todos los checkboxes asociados a la pregunta actual,
		// se iterará cada uno de ellos
		int answerId = 1;
		Iterator<CheckBox> checkboxes = this.currentCheckboxes.iterator();
		while(checkboxes.hasNext()) {
			// recuperar checkbox
			CheckBox checkbox = checkboxes.next();
			// poner en letra negra (por si ya habia sido chequeada antes)
			((TextView)this.currentCheckboxesTextViews.get(answerId-1)).setTextColor(Color.BLACK);
			// si se marco el checkbox se procesa
			if(checkbox.isChecked()) {
				// aumentar seleccionadas
				selected++;
				// si es correcta se marca verde
				if(this.test.question(this.currentQuestion).answerIsCorrect(answerId)) {
					((TextView)this.currentCheckboxesTextViews.get(answerId-1)).setTextColor(Color.GREEN);
				}
				// si es incorrecta se marca roja
				else {
					((TextView)this.currentCheckboxesTextViews.get(answerId-1)).setTextColor(Color.RED);
					correct = false; // se indica que se selecciono al menos una alternativa mala
				}
			}
			// avanzar contador
			answerId++;
		}
		// agregar puntaje solo si es la primera vez que se revisa esta pregunta
		if(!this.test.question(this.currentQuestion).checked) {
			// agregar como pregunta correcta si no hay alternativas incorrectas seleccionadas y
			// si se seleccionó la misma cantidad de checkboxes que de respuestas correctas
			if(correct && selected == this.test.question(this.currentQuestion).answersCorrect()) {
				this.test.corrects++;
			}
			// marcar la pregunta como que ya se reviso
			this.test.question(this.currentQuestion).checked = true;
		}
		// activar boton para ver la explicación (solo si existe una)
		if(this.test.question(this.currentQuestion).reason!=null && this.test.question(this.currentQuestion).reason.length()>0) {
			((Button)findViewById(R.id.btnReason)).setEnabled(true);
		}
		// activar boton para pasar a la siguiente pregunta
		((Button)findViewById(R.id.btnNext)).setEnabled(true);
	}
	
	/**
	 * Método que muestra la explicación de una pregunta
	 */
	private void showReason () {
		((TextView)findViewById(R.id.reason)).setText("Explicación: "+this.test.question(this.currentQuestion).reason);
	}
	
	/**
	 * Método que muestra la siguiente pregunta
	 */
	private void nextQuestion () {
		int nextQuestion = ++this.currentQuestion;
		// se pasa a la siguiente si existe una siguiente
		if(nextQuestion<=this.test.questions()) {
			this.showQuestion(nextQuestion);
		}
		// si no hay mas preguntas se muestran los resultados
		else {
			this.showResults();
		}
	}
    
	/**
	 * Método que muestra una pregunta
	 * @param n Número de pregunta que se desea mostrar
	 * @return Verdadero si la pregunta fue mostrada
	 */
	private boolean showQuestion (int n) {
		// cargar pregunta
		Question question = this.test.question(n);
		// si la pregunta no existe se retorna falso
		if(question==null) return false;
		// cargar layout principal de la pregunta
		setContentView(R.layout.question);
        // mostrar pregunta
        ((TextView)findViewById(R.id.question)).setText(n+".- "+question.question+" (dificultad: "+question.type+")");
        // si existe una imagen mostrarla
        if(question.image!=null) {
        	Bitmap bitmap = null;
        	// si existe la ruta completa de la imagen se carga directamente
        	if(question.image.startsWith("/")) {
        		bitmap = BitmapFactory.decodeFile(question.image);
        	}
            // si no existe la ruta es porque hay que sacarla del archivo zip
        	else {
        		InputStream is = this.test.getImage(question.image);
        		if(is!=null) {
        			bitmap = BitmapFactory.decodeStream(is);
        		}
        	}
        	// colocar imagen
        	if(bitmap!=null) {
        		((ImageView)findViewById(R.id.questionImage)).setImageBitmap(bitmap);
        	}
        }
        // agregar indicador de preguntas correctas existentes
        ((TextView)findViewById(R.id.answersCorrect)).setText("Seleccionar "+question.answersCorrect()+" alternativa(s):");
        // agregar alternativas
        char answerId = 'A';
        this.currentCheckboxes = new ArrayList<CheckBox>();
        this.currentCheckboxesTextViews = new ArrayList<TextView>();
        LinearLayout answersLayout = (LinearLayout) findViewById(R.id.answersLayout);
        Iterator<Answer> answers = question.answers.iterator();
        while(answers.hasNext()) {
        	// recuperar alternativa
        	Answer answer = answers.next();
        	// meter todo dentro de un linear layout
        	LinearLayout answerLayout = new LinearLayout(this);
        	// agregar checkbox
        	CheckBox checkbox = new CheckBox(this);
        	answerLayout.addView(checkbox);
        	// agregar alternativa
        	TextView textView = new TextView(this);
        	textView.setText((answerId++)+") "+answer.answer);
        	answerLayout.addView(textView);
        	// agregar layout de la respuesta al general de respuestas        	
        	answersLayout.addView(answerLayout);
        	// agregar al arreglo que lleva el control para revisar luego
        	this.currentCheckboxes.add(checkbox);
        	this.currentCheckboxesTextViews.add(textView);
        }
        // agregar listeners para botones
        ((Button)findViewById(R.id.btnCheck)).setOnClickListener(btnCheckListener);
        ((Button)findViewById(R.id.btnReason)).setOnClickListener(btnReasonListener);
        ((Button)findViewById(R.id.btnNext)).setOnClickListener(btnNextListener);
        // no se ha revisado aun la pregunta
        question.checked = false;
        // todo ok
        return true;
	}
	
	/**
	 * Método que muestra un resumen con los resultados de la prueba realizada
	 */
	private void showResults () {
		// cargar layout de resultados
		setContentView(R.layout.results);
		// mostrar resultados
		int percentage = (int)Math.round(((float)this.test.corrects/this.test.questions())*100);
		float grade = ((((float)this.test.corrects/this.test.questions())*6)+1);
		grade = (float)Math.round(grade * 10) / 10;
		((TextView)findViewById(R.id.results)).setText("Correctas: "+this.test.corrects+" / "+this.test.questions());
		((TextView)findViewById(R.id.resultsPercentage)).setText("Porcentaje: "+percentage+" / 100");
		((TextView)findViewById(R.id.resultsGrade)).setText("Nota: "+grade+" / 7.0");
	}
	
	/**
	 * Método que inicia la prueba
	 */
	private void start () {
		// reiniciar variables
		this.test.corrects = 0;
		this.currentQuestion = 1;
		// mostrar la primera pregunta
		this.showQuestion(this.currentQuestion);
	}
	
	/**
	 * Manejador de las opciones del menú presionadas
	 * @param item
	 * @return boolean Verdadero si todo ha ido ok
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_open:
				this.open();
				break;
			case R.id.menu_scan:
				this.scan();
				break;
			case R.id.menu_exit:
				this.finish();
				break;
		}
		return true;
	}
	
	/**
	 * Método para abrir el market de Google con la aplicación indicada
	 * @param appName
	 */
	private void goToMarket (String appName) {
		try {
		    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+appName)));
		} catch (android.content.ActivityNotFoundException anfe) {
		    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id="+appName)));
		}
	}
	
	/**
	 * Método que revisa si una aplicación se encuentra instalada
	 * @param packageName Nombre del paquete que se está buscando
	 * @return boolean, verdadero si existe
	 */
	private boolean isAppInstalled (String packageName) {
	    PackageManager pm = getPackageManager();
	    boolean installed = false;
	    try {
	       pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
	       installed = true;
	    } catch (PackageManager.NameNotFoundException e) {
	       installed = false;
	    }
	    return installed;
	}
	
	DialogInterface.OnClickListener dialogInstallFilemanager = new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	        switch (which) {
	        	case DialogInterface.BUTTON_POSITIVE:
	        		goToMarket("org.openintents.filemanager");
	        		break;
	        	case DialogInterface.BUTTON_NEGATIVE:
	        		break;
	        }
	    }
	};
	
	DialogInterface.OnClickListener dialogInstallBarcodeScanner = new DialogInterface.OnClickListener() {
	    @Override
	    public void onClick(DialogInterface dialog, int which) {
	        switch (which) {
	        	case DialogInterface.BUTTON_POSITIVE:
	        		goToMarket("com.google.zxing.client.android");
	        		break;
	        	case DialogInterface.BUTTON_NEGATIVE:
	        		break;
	        }
	    }
	};
	
	/**
	 * Método para acción del botón Abrir
	 * @param v
	 */
	public void open(View v) {
		this.open();
	}
	
	/**
	 * Método para acción del botón Escanear
	 * @param v
	 */
	public void scan(View v) {
		this.scan();
	}
	
	/**
	 * Método que abre un archivo, lanza actividad para OI File Manager
	 */
	private void open() {
		if (this.isAppInstalled("org.openintents.filemanager")) {
			// lanzar actividad para solicitar archivo
			Intent intent = new Intent("org.openintents.action.PICK_FILE");
			startActivityForResult(intent, ACTIVITY_FILEMANAGER);
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("MiTeSt requiere OI File Manager para abrir una prueba guardada en el equipo")
				.setPositiveButton("Instalar", dialogInstallFilemanager)
			    .setNegativeButton("Omitir", dialogInstallFilemanager).show();
		}
	}
	
	/**
	 * Método que escanea un código de barras QR para obtener la URL que se debe abrir
	 */
	private void scan() {
		if (this.isAppInstalled("com.google.zxing.client.android")) {
			// lanzar actividad para escanear código QR
			Intent intent = new Intent("com.google.zxing.client.android.SCAN");
			intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
			startActivityForResult(intent, ACTIVITY_BARCODE_SCANNER);
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("MiTeSt requiere Barcode Scanner para leer códigos QR")
				.setPositiveButton("Instalar", dialogInstallBarcodeScanner)
			    .setNegativeButton("Omitir", dialogInstallBarcodeScanner).show();
		}
	}
	
	/**
	 * Método que obtiene las pruebas que existen en el directorio MiTeSt
	 * @return
	 */
	/*private LinkedList<String> getLocalTests() {
		String path = this.getPath();
		File base_dir = new File(path);
		File usuarios[] = base_dir.listFiles();
		LinkedList<String> files = new LinkedList<String>();
		for (int i = 0; i < usuarios.length; i++) {
			if (usuarios[i].isDirectory()) {
				File categorias[] = usuarios[i].listFiles();
				for (int j = 0; j < categorias.length; j++) {
					if (categorias[j].isDirectory()) {
						File pruebas[] = categorias[j].listFiles();
						for (int k = 0; k < categorias.length; k++) {
							if (pruebas[k].isFile()) {
								files.add(pruebas[k].getAbsolutePath().substring(path.length()+1));
							}
						}
					} else {
						files.add(categorias[j].getAbsolutePath().substring(path.length()+1));
					}
				}
			} else {
				files.add(usuarios[i].getAbsolutePath().substring(path.length()+1));
			}
	    }
		return files;
	}
	
	private void populateLocalTests(LinkedList<String> files) {
		Spinner spinner_tests = (Spinner)findViewById(R.id.spinner_tests);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, files);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_tests.setAdapter(adapter);
		//spinner_tests.setSelection(selectedPosition);
		//Toast.makeText(this, filepath, Toast.LENGTH_LONG).show();
	}*/

	/**
	 * Método que crea una prueba a partir de un archivo y lanza la prueba
	 * @param location Archivo que se usará para crear la prueba
	 */
	private void open (String location) {
		// cargar preguntas (crear test)
		this.test = new Test(location.replace("file://", ""));
		// solo se inicia si se logró cargar un test
		if(!this.test.title.equals("")) {
			// mensaje de test cargado
			Toast.makeText(this, "Prueba \""+this.test.category+": "+this.test.title+"\" del autor "+this.test.author+" cargada.", Toast.LENGTH_LONG).show();
			// iniciar test
			this.start();
		}
	}
	
	private void openUrl(String uri) {
		String filepath = this.download(uri);
		if (filepath!=null) {
			this.open(filepath);
		} else {
			Toast.makeText(this, "No fue posible descargar la prueba", Toast.LENGTH_LONG).show();
		}
	}

	private String getPath() {
		if (this.app_data_path==null) {
			File externalStorage = Environment.getExternalStorageDirectory();
			if (externalStorage!=null) {
				this.app_data_path = externalStorage.getAbsolutePath()+"/MiTeSt";
			} else {
				File internalStorage = getApplicationContext().getFilesDir();
				this.app_data_path = internalStorage.getAbsolutePath()+"/MiTeSt";
			}
		}
		return this.app_data_path;
	}
	
	private String download(String uri) {
		try {

			// crear conexión
	        URL url = new URL(uri);
	        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
	        urlConnection.setRequestMethod("GET");
	        urlConnection.setDoOutput(true);
	        urlConnection.connect();

	        // determinar ruta y nombre del archivo 
	        String content_disposition = urlConnection.getHeaderField("Content-Disposition");
			String aux[] = content_disposition.split("_");
			String filename = aux[2].replace("\"", "");
			String path = this.getPath()+"/"+aux[0].replace("attachement; filename=\"", "")+"/"+aux[1];
			
	        // crear directorio (si no existe) y archivo
	        File dir = new File(path);
	        if (!dir.exists())
	        	dir.mkdirs();
	        File file = new File(dir, filename);
	        
	        // descargar archivo
	        FileOutputStream fileOutput = new FileOutputStream(file);
	        InputStream inputStream = urlConnection.getInputStream();
	        byte[] buffer = new byte[1024];
	        int bufferLength = 0;
	        while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
	                fileOutput.write(buffer, 0, bufferLength);
	        }
	        fileOutput.close();

	        // todo ok -> archivo descargado
	        return path+"/"+filename;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Manejador de resultados de llamadas a aplicaciones
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_OK) {
			if (requestCode == ACTIVITY_FILEMANAGER)
				this.open(intent.getData().toString());
			else if (requestCode == ACTIVITY_BARCODE_SCANNER)
				this.openUrl(intent.getStringExtra("SCAN_RESULT"));
		} else if (resultCode == RESULT_CANCELED) {
			// Handle cancel
		}
	}
	
}
