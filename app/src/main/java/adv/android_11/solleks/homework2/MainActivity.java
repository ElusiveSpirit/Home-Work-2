package adv.android_11.solleks.homework2;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements
            MainFragment.OnFragmentInteractionListener,
            GalleryFragment.OnDetailFragmentListener{

    private static final String RUNNING_FRAGMENT = "adv.android_11.solleks.homework2.MainActivity.RUNNING_FRAGMENT";
    private String mLastFragmentRunning;

    private MainFragment mainFragment;
    private GalleryFragment galleryFragment;
    private DetailFragment detailFragment;

    private static int DisplayWidth;
    private static int DisplayHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FragmentManager manager = getSupportFragmentManager();
        List<Fragment> fragments = manager.getFragments();
        // TODO Найти: куда при загадочных обстоятельствах исчезет фрагмент
        Fragment fragment1 = manager.findFragmentByTag(DetailFragment.TAG);
        if (fragments == null) {
            MainFragment.IMAGE_HEIGHT = MainFragment.IMAGE_WIDTH = getResources().getDimensionPixelOffset(R.dimen.preview_image_height);

            DirLoader loader = new DirLoader();
            loader.execute(Environment.getExternalStorageDirectory());
        } else {
            FragmentTransaction fragmentTransaction = manager.beginTransaction();
            for (Fragment fragment : fragments) {
                if (fragment != null) {
                    fragmentTransaction.attach(fragment);
                    switch (fragment.getTag()) {
                        case MainFragment.TAG:
                            mainFragment = (MainFragment) fragment;
                            break;
                        case GalleryFragment.TAG:
                            galleryFragment = (GalleryFragment) fragment;
                            break;
                        case DetailFragment.TAG:
                            detailFragment = (DetailFragment) fragment;
                    }
                }
            }
            fragmentTransaction.commit();
        }
        if (galleryFragment == null) {
            galleryFragment = new GalleryFragment();
            GalleryFragment.IMAGE_WIDTH = GalleryFragment.IMAGE_HEIGHT = getResources().getDimensionPixelSize(R.dimen.image_height);
        }
        if (detailFragment == null)
            detailFragment = new DetailFragment();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

       //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(String fileName) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        if (getSupportFragmentManager().findFragmentByTag(GalleryFragment.TAG) == null) {
            fragmentTransaction
                    .replace(R.id.fragmentContainer, galleryFragment, GalleryFragment.TAG)
                    .addToBackStack(null)
                    .commit();
        } else {
            fragmentTransaction
                    .hide(mainFragment)
                    .show(galleryFragment)
                    .addToBackStack(null)
                    .commit();
        }
        if (galleryFragment.getOpenedDir() == null || !galleryFragment.getOpenedDir().equals(fileName))
            galleryFragment.changePath(fileName);
    }

    @Override
    public void onDetailFragmentOpener(String fileName) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        if (getSupportFragmentManager().findFragmentByTag(DetailFragment.TAG) == null) {
            fragmentTransaction
                    .replace(R.id.fragmentContainer, detailFragment, DetailFragment.TAG)
                    .addToBackStack(null)
                    .commit();
        } else {
            fragmentTransaction
                    .hide(galleryFragment)
                    .show(detailFragment)
                    .addToBackStack(null)
                    .commit();
        }
        DisplayHeight = findViewById(R.id.fragmentContainer).getMeasuredHeight();
        DisplayWidth = findViewById(R.id.fragmentContainer).getMeasuredWidth();
        detailFragment.setImageFile(fileName);
    }

    public static int getDisplayWidth() {
        return DisplayWidth;
    }
    public static int getDisplayHeight() {
        return DisplayHeight;
    }

    class DirLoader extends AsyncTask<File, Image, ArrayList<String>> {

        @Override
        protected ArrayList<String> doInBackground(File... params) {


            File rootDir = params[0];

            return saveFilesFromDir(rootDir);
        }

        @Override
        protected void onProgressUpdate(Image... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(ArrayList<String> strings) {
            super.onPostExecute(strings);
            mainFragment = MainFragment.newInstance(strings);

            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction
                    .add(R.id.fragmentContainer, mainFragment, MainFragment.TAG)
                    .addToBackStack(null)
                    .commit();
            mLastFragmentRunning = MainFragment.TAG;
        }

        public ArrayList<String> saveFilesFromDir(File rootDir) {
            ArrayList<String> data = new ArrayList<>();

            File[] dirList = rootDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (!name.contains("."));
                }
            });
            File[] imageList = rootDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"));
                }
            });

            ArrayList<File> fileList = new ArrayList<>();
            fileList.addAll(Arrays.asList(imageList));
            fileList = sort(fileList);
            if (fileList.size() > 0) {
                data.add(fileList.get(fileList.size() - 1).getAbsolutePath());
            }
            for (File dir : dirList) {
                String subDirs[] = dir.getAbsolutePath().split("/");
                boolean contains = false;
                for (String subDir : subDirs)
                    if (subDir.equals("Music")) {
                        contains = true;
                        break;
                    }
                if (!contains)
                    data.addAll(saveFilesFromDir(dir));
            }

            return data;
        }

        public ArrayList<File> sort(ArrayList<File> input) {
            if (input.size() == 0) {
                return input;
            }
            File head = input.get(0);
            input.remove(0);
            ArrayList<File> right = new ArrayList<>(input.size()/2);
            ArrayList<File> left = new ArrayList<>(input.size()/2);

            for(File element: input) {
                if (element.lastModified() > head.lastModified()) {
                    right.add(element);
                } else {
                    left.add(element);
                }
            }

            ArrayList<File> result = new ArrayList<>(input.size());
            result.addAll(sort(left));
            result.add(head);
            result.addAll(sort(right));
            return result;
        }
    }

    @Override
    public void onBackPressed() {
        android.support.v4.app.FragmentManager manager = getSupportFragmentManager();

        List<Fragment> list = manager.getFragments();

        if (manager.findFragmentByTag(MainFragment.TAG).isVisible()) {
            finish();
        } else if (manager.findFragmentByTag(GalleryFragment.TAG).isVisible()) {
          /*  getSupportFragmentManager().beginTransaction()
                    .attach(galleryFragment)
                    .commit();*/
        } else if (manager.findFragmentByTag(DetailFragment.TAG).isVisible()) {
            detailFragment.cancelLoading();
        }

        super.onBackPressed();
    }
}
