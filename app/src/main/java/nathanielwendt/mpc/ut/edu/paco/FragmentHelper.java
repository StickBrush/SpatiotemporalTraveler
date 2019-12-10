package nathanielwendt.mpc.ut.edu.paco;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * Created by nathanielwendt on 11/25/16.
 */
public class FragmentHelper {
    private FragmentManager manager;
    private int containerViewId;
    private Fragment lastFragment;

    public FragmentHelper(int containerViewId, FragmentManager manager){
        this.containerViewId = containerViewId;
        this.manager = manager;
    }

    public void show(String tag, Fragment fragment){
        this.show(tag, fragment, false);
    }

    public void show(String tag, Fragment fragment, boolean destroyLast){
        FragmentTransaction ft = manager.beginTransaction();

        if(lastFragment != null){
            if(destroyLast){
                ft.remove(lastFragment);
            } else {
                ft.hide(lastFragment);
            }
        }

        Fragment next = manager.findFragmentByTag(tag);
        if(next != null){
            ft.show(next);
            lastFragment = next;
        } else {
            ft.add(containerViewId, fragment, tag);
            lastFragment = fragment;
        }
        ft.commit();
    }



}
