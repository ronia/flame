package flame.client;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.*;

/**
 * Small dialogue window that gets login information (ID and password) from Architect
 * 
 * @author 					<a href=mailto:jaeyounb@usc.edu>Jae young Bang</a>
 * @version					2013.05
 */
public class LoginGUI {
	
	private String server;
	private String username;
	private String password;
	private String initUsername;
	
	private Display display;
	private Shell shell;
	
	private Text textUsername;
	private Text textPassword;

	public LoginGUI(String iun) throws SWTException {
		initUsername = iun;
		initComponents();
	}
	
	public Shell get_shell() {
		return shell;
	}
	
	protected ImageData loadImageData (String imagePath) throws SWTException {
        InputStream stream = this.getClass().getResourceAsStream (imagePath);
        if (stream == null) throw new SWTException (imagePath + " not found.");
        ImageData imageData = null;
        try {
             imageData = new ImageData (stream);
        } catch (SWTException ex) {
        } finally {
             try {
                  stream.close ();
             } catch (IOException ex) {}
        }
        return imageData;
   }
		
	private void initComponents() throws SWTException {
		display = new Display();
		
		shell = new Shell(display, SWT.NO_TRIM);
		
		// reads the screen DPI setting and adjust magnification
		int mag = 1;
		ImageData img = null;
	
		if (shell.getDisplay().getDPI().x > 96) {
			mag = 2;
			img = loadImageData("/login_2x.bmp");
					//new ImageData(this.getClass().getResourceAsStream("res/login_2x.bmp"));
		} else {
			mag = 1;
			img = loadImageData("/login_1x.bmp");
		}
		
		shell.setText("FLAME Client");
		shell.setSize(500 * mag, 285 * mag);
		shell.setBackgroundImage(new Image(display, img));
		
		Monitor primary = display.getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = shell.getBounds();
    
	    int x = bounds.x + (bounds.width - rect.width) / 2;
	    int y = bounds.y + (bounds.height - rect.height) / 2;
		
		shell.setLocation(x, y);
		
		Label labelUsername = new Label(shell, SWT.NONE);
		labelUsername.setBackground(new Color(display, new RGB(255,255,255)));
		labelUsername.setText("Username");
		labelUsername.setSize(100 * mag, 20 * mag);
		labelUsername.setLocation(160 * mag, 150 * mag);
		
		Label labelPassword = new Label(shell, SWT.NONE);
		labelPassword.setBackground(new Color(display, new RGB(255,255,255)));
		labelPassword.setText("Password");
		labelPassword.setSize(100 * mag, 20 * mag);
		labelPassword.setLocation(160 * mag, 180 * mag);

		textUsername = new Text(shell, SWT.BORDER);
		textUsername.setText(initUsername);
		textUsername.setBounds(260 * mag,145 * mag,150 * mag,20 * mag); 
		textUsername.setTextLimit(20);
		
		textPassword = new Text(shell, SWT.BORDER);
		textPassword.setEchoChar('*');
		textPassword.setText("");
		textPassword.setBounds(260 * mag,175 * mag,150 * mag,20 * mag); 
		textPassword.setTextLimit(20);
		
		Button buttonLogin = new Button(shell, SWT.PUSH);
		buttonLogin.setLocation(160 * mag, 220 * mag);
		buttonLogin.setSize(130 * mag, 23 * mag);
		buttonLogin.setText("Log In");
		
		Button buttonExit = new Button(shell, SWT.PUSH);
		buttonExit.setLocation(300 * mag, 220 * mag);
		buttonExit.setSize(130 * mag, 23 * mag);
		buttonExit.setText("Exit");
		
		buttonLogin.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				username = textUsername.getText();
				password = textPassword.getText();
				
				if(!username.equals("")) {
					shell.dispose();
				}
			}
		});
		
		buttonExit.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				shell.dispose();
				System.exit(0);
			}
		});
		
		Control[] list = new Control[] {textUsername, textPassword, buttonLogin};
		shell.setTabList(list);

		shell.open();
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}
	
	public String get_server() { return server; }
	public String get_username() { return username; }
	public String get_password() { return password; }
}
