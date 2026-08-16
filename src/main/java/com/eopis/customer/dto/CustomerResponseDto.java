package com.eopis.customer.dto;

import com.eopis.customer.entity.Customer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CustomerResponseDto {
    private Long id;
    private UUID userId;
    private String customerNumber;
    private String firstName;
    private String lastName;
    private String phone;
    private String tier;
    private List<AddressDto> addresses;
    private OffsetDateTime createdAt;

    public CustomerResponseDto() {
    }

    public static CustomerResponseDto fromEntity(Customer customer) {
        CustomerResponseDto dto = new CustomerResponseDto();
        dto.setId(customer.getId());
        dto.setUserId(customer.getUserId());
        dto.setCustomerNumber(customer.getCustomerNumber());
        dto.setFirstName(customer.getFirstName());
        dto.setLastName(customer.getLastName());
        dto.setPhone(customer.getPhone());
        dto.setTier(customer.getTier());
        dto.setCreatedAt(customer.getCreatedAt());
        if (customer.getAddresses() != null) {
            dto.setAddresses(customer.getAddresses().stream()
                    .map(AddressDto::fromEntity)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public static class AddressDto {
        private Long id;
        private String street;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private boolean defaultBilling;
        private boolean defaultShipping;

        public AddressDto() {}

        public static AddressDto fromEntity(com.eopis.customer.entity.Address address) {
            AddressDto dto = new AddressDto();
            dto.setId(address.getId());
            dto.setStreet(address.getStreet());
            dto.setCity(address.getCity());
            dto.setState(address.getState());
            dto.setPostalCode(address.getPostalCode());
            dto.setCountry(address.getCountry());
            dto.setDefaultBilling(address.isDefaultBilling());
            dto.setDefaultShipping(address.isDefaultShipping());
            return dto;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public boolean isDefaultBilling() { return defaultBilling; }
        public void setDefaultBilling(boolean defaultBilling) { this.defaultBilling = defaultBilling; }
        public boolean isDefaultShipping() { return defaultShipping; }
        public void setDefaultShipping(boolean defaultShipping) { this.defaultShipping = defaultShipping; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getCustomerNumber() { return customerNumber; }
    public void setCustomerNumber(String customerNumber) { this.customerNumber = customerNumber; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
    public List<AddressDto> getAddresses() { return addresses; }
    public void setAddresses(List<AddressDto> addresses) { this.addresses = addresses; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
