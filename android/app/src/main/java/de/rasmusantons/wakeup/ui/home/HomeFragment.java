package de.rasmusantons.wakeup.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import de.rasmusantons.wakeup.R;
import de.rasmusantons.wakeup.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        ListView wakeupList = root.findViewById(R.id.wakeup_list);
        String[] testItems = {"App UI not implemented", "for now use the website version"};
        ArrayAdapter<String> testAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, testItems);
        wakeupList.setAdapter(testAdapter);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
