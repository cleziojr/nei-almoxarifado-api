package com.ufrn.nei.almoxarifadoapi.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import com.ufrn.nei.almoxarifadoapi.exception.CreateEntityException;
import com.ufrn.nei.almoxarifadoapi.exception.EntityNotFoundException;
import com.ufrn.nei.almoxarifadoapi.exception.ItemNotActiveException;
import com.ufrn.nei.almoxarifadoapi.exception.NotAvailableQuantityException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ufrn.nei.almoxarifadoapi.dto.item.ItemCreateDTO;
import com.ufrn.nei.almoxarifadoapi.dto.item.ItemResponseDTO;
import com.ufrn.nei.almoxarifadoapi.dto.item.ItemUpdateDTO;
import com.ufrn.nei.almoxarifadoapi.dto.mapper.ItemMapper;
import com.ufrn.nei.almoxarifadoapi.entity.ItemEntity;
import com.ufrn.nei.almoxarifadoapi.repository.ItemRepository;

import jakarta.transaction.Transactional;

@Service
public class ItemService {
    @Autowired
    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional
    public List<ItemResponseDTO> findAllItems() {
        return ItemMapper.toListResponseDTO(itemRepository.findAllByActiveTrue());
    }

    @Transactional
    public ItemResponseDTO findById(Long id) {
        return ItemMapper.toResponseDTO(itemRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException(String.format("Item não encontrado com id=%s", id))));
    }

    @Transactional
    public ItemEntity findByTagging(Long itemTagging) {
        return itemRepository.findByItemTagging(itemTagging).orElseThrow(
                () -> new EntityNotFoundException(String.format("Item não encontrado com tagging=%s", itemTagging)));
    }

    @Transactional
    public ItemResponseDTO createItem(ItemCreateDTO data) {
        ItemEntity item = ItemMapper.toItem(data);

        if (item == null) {
            throw new CreateEntityException("Erro na criação da entidade item");
        }

        try {
            item = findByTagging(data.getItemTagging());

            item.setQuantityAvailable(item.getQuantityAvailable() + data.getQuantityAvailable());
        } catch (EntityNotFoundException ignored) {}

        item.setActive(true);
        itemRepository.save(item);

        return ItemMapper.toResponseDTO(item);
    }

    @Transactional
    public ItemResponseDTO updateItem(Long id, ItemUpdateDTO data) {
        ItemEntity item = ItemMapper.toItem(findById(id));

        if (data.getName() != null && !data.getName().isBlank()) {
            item.setName(data.getName());
        }
        if (data.getItemTagging() != null) {
            item.setItemTagging(data.getItemTagging());
        }
        item.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

        itemRepository.save(item);
        return ItemMapper.toResponseDTO(item);
    }

    @Transactional
    public ItemResponseDTO sumLendQuantity(Long id, Integer itemsToLend) {
        ItemEntity item = ItemMapper.toItem(findById(id));

        if (itemsToLend <= item.getQuantityAvailable()) {
            item.setQuantityLend(item.getQuantityLend() + itemsToLend);
            item.setQuantityAvailable(item.getQuantityAvailable() - itemsToLend);

            itemRepository.save(item);
            return ItemMapper.toResponseDTO(item);
        } else {
            throw new NotAvailableQuantityException("Não há itens suficientes para realizar este empréstimo.");
        }
    }

    @Transactional
    public ItemResponseDTO subtractLendQuantity(Long id, Integer itemsToLend) {
        ItemEntity item = ItemMapper.toItem(findById(id));

        // Caso a quantidade de itens emprestados enviada seja maior que a cadastrada
        if (itemsToLend > item.getQuantityLend()) {
            int diff = itemsToLend - item.getQuantityLend();
            itemsToLend -= diff;
        }

        item.setQuantityLend(item.getQuantityLend() - itemsToLend);
        item.setQuantityAvailable(item.getQuantityAvailable() + itemsToLend);

        itemRepository.save(item);
        return ItemMapper.toResponseDTO(item);
    }

    @Transactional
    public void deleteItem(Long id, Integer quantity) {
        ItemEntity item = ItemMapper.toItem(findById(id));

        if (item.getActive().equals(false)) {
            throw new ItemNotActiveException();
        } else if (quantity > item.getQuantityAvailable()) {
            throw new NotAvailableQuantityException("Não há itens disponiveis suficientes para realizar a exclusão.");
        }

        item.setQuantityAvailable(item.getQuantityAvailable() - quantity);

        if (item.getQuantityAvailable() == 0 && item.getQuantityLend() == 0) {
            item.setActive(false);
        }

        itemRepository.save(item);
    }
}
