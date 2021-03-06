/* 
* This file is part of Track & Graph
* 
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.DialogFragment
import com.samco.trackandgraph.R

class YesCancelDialogFragment : DialogFragment() {
    lateinit var title: String
    private lateinit var listener: YesCancelDialogListener

    interface YesCancelDialogListener {
        fun onDialogYes(dialog: YesCancelDialogFragment)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) : Dialog {
        title = arguments?.getString("title") ?: ""
        return activity?.let {
            listener = parentFragment as YesCancelDialogListener
            var builder = AlertDialog.Builder(it)
            builder.setMessage(title)
                .setPositiveButton(R.string.yes) { _, _ -> listener.onDialogYes(this) }
                .setNegativeButton(R.string.cancel) { _, _ -> {} }
            val alertDialog = builder.create()
            alertDialog.setOnShowListener {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(context!!, R.color.secondaryColor))
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(context!!, R.color.toolBarTextColor ))
            }
            alertDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}