package com.example.calculator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.calculator.R;
import com.google.android.material.card.MaterialCardView;
import android.widget.ImageButton;

import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {
    private final List<Message> messages;
    private final Context context;

    public MessagesAdapter(List<Message> messages, Context context) {
        this.messages = messages;
        this.context = context;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView messageText;
        ImageButton copyButton;
        View senderIndicator;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.messageCard);
            messageText = itemView.findViewById(R.id.messageText);
            copyButton = itemView.findViewById(R.id.copyButton);
            senderIndicator = itemView.findViewById(R.id.senderIndicator);
        }

        public void bind(Message message) {
            // Set text with HTML support for links
            Spanned text = Html.fromHtml(message.text.replace("\n", "<br>"), Html.FROM_HTML_MODE_LEGACY);
            messageText.setText(text);
            messageText.setMovementMethod(LinkMovementMethod.getInstance());

            // Style based on sender
            if (message.isUser) {
                cardView.setCardBackgroundColor(cardView.getContext().getColor(R.color.primary));
                senderIndicator.setBackgroundColor(ContextCompat.getColor(cardView.getContext(), R.color.primary));
                // Align to end
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) cardView.getLayoutParams();
                params.gravity = android.view.Gravity.END;
                cardView.setLayoutParams(params);
            } else {
                cardView.setCardBackgroundColor(cardView.getContext().getColor(R.color.surface));
                senderIndicator.setBackgroundColor(ContextCompat.getColor(cardView.getContext(), R.color.primary));
                // Align to start
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) cardView.getLayoutParams();
                params.gravity = android.view.Gravity.START;
                cardView.setLayoutParams(params);
            }

            // Copy button
            copyButton.setOnClickListener(v -> copyToClipboard(message.text));
        }
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Message", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show();
    }
}