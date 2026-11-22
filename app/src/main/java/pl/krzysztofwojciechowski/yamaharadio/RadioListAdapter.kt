package pl.krzysztofwojciechowski.yamaharadio

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import pl.krzysztofwojciechowski.yamaharadio.model.RadioPreset

class RadioListAdapter : RecyclerView.Adapter<RadioListAdapter.ViewHolder>() {
    private var dataSet: List<RadioPreset> = listOf()
    var clickListener: ((RadioPreset) -> Unit)? = null

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        val nameTextView: TextView
        var preset: RadioPreset? = null

        init {
            view.setOnClickListener(this)
            nameTextView = view.findViewById(R.id.listItemName)
        }

        fun bind(preset: RadioPreset) {
            nameTextView.text = preset.name
            this.preset = preset
        }

        override fun onClick(p0: View?) {
            if (clickListener != null && preset != null) {
                clickListener?.invoke(preset!!)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun update(newData: List<RadioPreset>) {
        dataSet = newData
        notifyDataSetChanged()
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.list_item, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.bind(dataSet[position])
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size
}