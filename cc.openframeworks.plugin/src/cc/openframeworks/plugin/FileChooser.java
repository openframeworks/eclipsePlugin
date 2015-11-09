package cc.openframeworks.plugin;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
 
public class FileChooser extends Composite {
 
	Text mText;
	Button mButton;
	String title = null;
	SelectionListener selectionListener;
	boolean directory;
 
	public FileChooser(Composite parent, boolean directory) {
		super(parent, SWT.NONE);
		this.directory = directory;
		createContent();
	}
 
	public void createContent() {
		GridLayout layout = new GridLayout(2, false);
		setLayout(layout);
 
		mText = new Text(this, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		mText.setLayoutData(gd);
 
 
		mButton = new Button(this, SWT.NONE);
		mButton.setText("...");
		mButton.addSelectionListener(new SelectionListener() {
 
			public void widgetDefaultSelected(SelectionEvent e) {
				if(selectionListener!=null){
					selectionListener.widgetDefaultSelected(e);
				}
			}
 
			public void widgetSelected(SelectionEvent e) {
				if(directory){
					DirectoryDialog dlg = new DirectoryDialog(mButton.getShell(),  SWT.OPEN  );
					dlg.setText("Choose path");
					dlg.setFilterPath(mText.getText());
					String path = dlg.open();
					if (path == null) return;
					mText.setText(path);
				}else{
					FileDialog dlg = new FileDialog(mButton.getShell(),  SWT.OPEN  );
					dlg.setText("Open");
					dlg.setFilterPath(mText.getText());
					String path = dlg.open();
					if (path == null) return;
					mText.setText(path);
				}
				if(selectionListener!=null){
					selectionListener.widgetSelected(e);
				}
			}
		});
	}
	
	public void addSelectionListener(SelectionListener listener){
		this.selectionListener = listener;
	}
	
	public void addModifyListener(ModifyListener listener){
		mText.addModifyListener(listener);
	}
 
	public String getText() {
		return mText.getText();
 
	}
 
	public Text getTextControl() {
		return mText;		
	}
 
	public File getFile() {
		String text = mText.getText();
		if (text.length() == 0) return null;
		return new File(text);
	}
 
	public String getTitle() {
		return title;
	}
 
	public void setTitle(String title) {
		this.title = title;
	}
	 
	public void setText(String folder) {
		mText.setText(folder);
	}
}