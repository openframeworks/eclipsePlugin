package cc.openframeworks.plugin.wizards;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.osgi.framework.Bundle;

import cc.openframeworks.plugin.Activator;
import cc.openframeworks.plugin.FileChooser;
import cc.openframeworks.plugin.Utils;

/**
 * The "New" wizard page allows setting the container for the new file as well
 * as the file name. The page will only accept file name without the extension
 * OR with the extension that matches the expected one (mpe).
 */

public class OFNewWizardPage extends WizardPage {
	private FileChooser folderChooser;
	private Text fileText;
	private SelectAddonPage addonsPage;

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public OFNewWizardPage(ISelection selection, SelectAddonPage addonsPage) {
		super("Create project");
		setTitle("openFrameworks application");
		setDescription("This wizard creates a new openFrameworks application project, you can include addons by pressin Next or directly create an empty project pressing Finish.");
	    Bundle bundle = Platform.getBundle("cc.openframeworks.plugin");
	    setImageDescriptor(ImageDescriptor.createFromURL(bundle.getEntry("icons/ofw-logo.png")));
	    this.addonsPage = addonsPage;
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		Label label = new Label(container, SWT.NULL);
		label.setText("&Folder:");

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		
		folderChooser = new FileChooser(container, true);
		folderChooser.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
			
			}
			
			@Override
			public void keyReleased(KeyEvent e) {
		    	dialogChanged();
			}
	    });
		folderChooser.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				dialogChanged();
			}

			@Override
			public void widgetSelected(SelectionEvent arg0) {
				dialogChanged();
				
			}
			
		});
		folderChooser.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		folderChooser.setLayoutData(gd);
		label = new Label(container, SWT.NULL);
		label.setText("&Project name:");

		fileText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fileText.setLayoutData(gd);
		fileText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});
		initialize();
		dialogChanged();
		setControl(container);
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */

	private void initialize() {
		fileText.setText("MyNewApplication");
		String ofRoot = Activator.getDefault().getPreferenceStore().getString("OF_ROOT");
		if(ofRoot!=null && ofRoot!=""){
			setProjectFolder(ofRoot + "/apps/myApps");
		}else{
			setMessage("The default OF root is not set, you can set it in Window > Preferences > openFrameworks",WizardPage.WARNING);
		}
	}

	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		String projectName = getProjectName();
		File folder = getProjectFolder();

		String ofRoot = Activator.getDefault().getPreferenceStore().getString("OF_ROOT");
		if((ofRoot==null || ofRoot.equals("")) && (folder==null || folder.getName().equals(""))){
			setMessage("The default OF root is not set, you can set it in Window > Preferences > openFrameworks",WizardPage.WARNING);
			return;
		}
		if (folder==null || !folder.exists()) {
			updateStatus("Project folder must exist");
			return;
		}
		if (projectName.length() == 0) {
			updateStatus("File name must be specified");
			return;
		}
		if (projectName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus("File name must be valid");
			return;
		}

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		if(root.getProject(projectName).exists()){
			updateStatus("There's already a project by this name, please enter a different one");
			return;
		}
		
		if(getProjectFullPath().exists()){
			File addonsMake = new File(getProjectFullPath().getAbsolutePath() + "/addons.make");
			if(!addonsMake.exists()){
				try {
					addonsMake.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			Set<String> currentAddons = Utils.parseAddonsMake(addonsMake);
			addonsPage.setAddons(currentAddons);
			setMessage("The project already exists in the file system, it's settings will be overwriten",WizardPage.WARNING);
			setPageComplete(true);
		}else{
			addonsPage.setAddons(new HashSet<String>());
			setMessage("This wizard creates a new openFrameworks application project, you can include addons by pressing Next or directly create an empty project pressing Finish.",WizardPage.NONE);
			setPageComplete(true);
		}
		
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public File getProjectFolder() {
		return folderChooser.getFile();
	}

	public String getProjectName() {
		return fileText.getText();
	}
	
	public void setProjectFolder(String folder){
		if(folderChooser!=null){
			folderChooser.setText(folder);
			dialogChanged();
		}
	}
	
	public File getProjectFullPath(){
		if(getProjectFolder().getName().equals(getProjectName())){
			return getProjectFolder();
		}else{
			return new File(getProjectFolder().getAbsolutePath()+"/"+getProjectName());
		}
	}
	
}